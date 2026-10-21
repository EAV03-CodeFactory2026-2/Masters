package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.BloqueHorarioDTO;
import com.udea.masters.controllers.DTOs.DiaHorarioRequest;
import com.udea.masters.controllers.DTOs.DiaHorarioResponse;
import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalResponse;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.DiaSemana;
import com.udea.masters.entities.HorarioProveedor;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.HorarioInvalidoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.repositories.HorarioProveedorRepository;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.security.IAuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {

    private static final Integer PROVEEDOR_ID = 7;
    private static final String ROL_PROVEEDOR = "Proveedor";

    @Mock
    private HorarioProveedorRepository horarioRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private HorarioValidator horarioValidator;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-10T15:30:00Z"), ZoneOffset.UTC);

    private HorarioService horarioService;
    private HorarioSemanalRequest request;
    private PerfilProveedor perfil;

    @BeforeEach
    void setUp() {
        horarioService = new HorarioService(horarioRepository, perfilProveedorRepository, horarioValidator,
                authenticationFacade, clock);

        request = HorarioSemanalRequest.builder()
                .dias(List.of(DiaHorarioRequest.builder()
                        .dia("LUNES")
                        .activo(true)
                        .bloques(List.of(new BloqueHorarioDTO("08:00", "12:00")))
                        .build()))
                .build();

        perfil = PerfilProveedor.builder().usuarioId(PROVEEDOR_ID).especialidad("Barbería").build();
    }

    private void autenticarProveedorConPerfil() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(true);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(PROVEEDOR_ID);
        when(perfilProveedorRepository.existsById(PROVEEDOR_ID)).thenReturn(true);
    }

    private static BloqueHorario bloque(DiaSemana dia, String inicio, String fin) {
        return BloqueHorario.builder()
                .diaSemana(dia)
                .horaInicio(LocalTime.parse(inicio))
                .horaFin(LocalTime.parse(fin))
                .build();
    }

    private static DiaHorarioResponse diaRespuesta(HorarioSemanalResponse response, DiaSemana dia) {
        return response.getDias().get(dia.ordinal());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoConsultaSinRolProveedor() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> horarioService.consultarMiHorario());

        assertEquals(HorarioService.MSG_SOLO_PROVEEDOR, ex.getMessage());
        verify(authenticationFacade, never()).getAuthenticatedUserId();
        verifyNoInteractions(horarioRepository, perfilProveedorRepository);
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoConsultaSinUsuarioAutenticado() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(true);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> horarioService.consultarMiHorario());

        assertEquals(HorarioService.MSG_SIN_PERFIL, ex.getMessage());
        verifyNoInteractions(horarioRepository, perfilProveedorRepository);
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoConsultaSinPerfilProveedor() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(true);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(PROVEEDOR_ID);
        when(perfilProveedorRepository.existsById(PROVEEDOR_ID)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> horarioService.consultarMiHorario());

        assertEquals(HorarioService.MSG_SIN_PERFIL, ex.getMessage());
        verifyNoInteractions(horarioRepository);
    }

    @Test
    void debeRetornarSieteDiasInactivos_cuandoNuncaSeGuardoHorario() {
        autenticarProveedorConPerfil();
        when(horarioRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.empty());

        HorarioSemanalResponse response = horarioService.consultarMiHorario();

        assertNull(response.getMensaje());
        assertNull(response.getFechaActualizacion());
        assertEquals(7, response.getDias().size());
        assertTrue(response.getDias().stream().noneMatch(DiaHorarioResponse::isActivo));
        assertTrue(response.getDias().stream().allMatch(d -> d.getBloques().isEmpty()));
        assertEquals("LUNES", response.getDias().get(0).getDia());
        assertEquals("Miércoles", response.getDias().get(2).getNombre());
        assertEquals("DOMINGO", response.getDias().get(6).getDia());
    }

    @Test
    void debeRetornarHorarioGuardado_cuandoProveedorTieneHorario() {
        autenticarProveedorConPerfil();
        OffsetDateTime fecha = OffsetDateTime.parse("2026-03-01T10:00:00Z");
        HorarioProveedor horario = HorarioProveedor.builder()
                .idHorario(3)
                .proveedor(perfil)
                .fechaActualizacion(fecha)
                .bloques(new ArrayList<>(List.of(
                        bloque(DiaSemana.LUNES, "14:00", "18:00"),
                        bloque(DiaSemana.LUNES, "08:00", "12:00"),
                        bloque(DiaSemana.VIERNES, "09:15", "11:45"))))
                .build();
        when(horarioRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horario));

        HorarioSemanalResponse response = horarioService.consultarMiHorario();

        assertNull(response.getMensaje());
        assertEquals(fecha, response.getFechaActualizacion());
        DiaHorarioResponse lunes = diaRespuesta(response, DiaSemana.LUNES);
        assertTrue(lunes.isActivo());
        assertEquals(List.of(new BloqueHorarioDTO("08:00", "12:00"), new BloqueHorarioDTO("14:00", "18:00")),
                lunes.getBloques());
        assertEquals(List.of(new BloqueHorarioDTO("09:15", "11:45")),
                diaRespuesta(response, DiaSemana.VIERNES).getBloques());
        assertFalse(diaRespuesta(response, DiaSemana.MARTES).isActivo());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoGuardaSinRolProveedor() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> horarioService.guardarMiHorario(request));

        assertEquals(HorarioService.MSG_SOLO_PROVEEDOR, ex.getMessage());
        verifyNoInteractions(horarioValidator, horarioRepository, perfilProveedorRepository);
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoGuardaSinPerfilProveedor() {
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(true);
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(PROVEEDOR_ID);
        when(perfilProveedorRepository.existsById(PROVEEDOR_ID)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> horarioService.guardarMiHorario(request));

        assertEquals(HorarioService.MSG_SIN_PERFIL, ex.getMessage());
        verifyNoInteractions(horarioValidator, horarioRepository);
    }

    @Test
    void debePropagarHorarioInvalido_cuandoValidadorRechazaHorario() {
        autenticarProveedorConPerfil();
        when(horarioValidator.validar(request))
                .thenThrow(new HorarioInvalidoException(HorarioValidator.MSG_MAX_BLOQUES, "LUNES", null));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class,
                () -> horarioService.guardarMiHorario(request));

        assertEquals(HorarioValidator.MSG_MAX_BLOQUES, ex.getMessage());
        assertEquals("LUNES", ex.getDia());
        verify(horarioRepository, never()).findByProveedorUsuarioId(any());
        verify(horarioRepository, never()).save(any(HorarioProveedor.class));
    }

    @Test
    void debeReemplazarBloques_cuandoProveedorYaTieneHorario() {
        autenticarProveedorConPerfil();
        BloqueHorario anterior = bloque(DiaSemana.MARTES, "07:00", "09:00");
        HorarioProveedor existente = HorarioProveedor.builder()
                .idHorario(3)
                .proveedor(perfil)
                .fechaActualizacion(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .bloques(new ArrayList<>(List.of(anterior)))
                .build();
        BloqueHorario nuevo = bloque(DiaSemana.LUNES, "08:00", "12:00");
        when(horarioValidator.validar(request)).thenReturn(new ArrayList<>(List.of(nuevo)));
        when(horarioRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(existente));
        when(horarioRepository.save(existente)).thenReturn(existente);

        HorarioSemanalResponse response = horarioService.guardarMiHorario(request);

        assertEquals(HorarioService.MSG_GUARDADO, response.getMensaje());
        assertEquals(OffsetDateTime.now(clock), response.getFechaActualizacion());
        assertEquals(List.of(nuevo), existente.getBloques());
        assertSame(existente, nuevo.getHorario());
        assertTrue(diaRespuesta(response, DiaSemana.LUNES).isActivo());
        assertFalse(diaRespuesta(response, DiaSemana.MARTES).isActivo());
        verify(perfilProveedorRepository, never()).findById(any());
    }

    @Test
    void debeCrearHorario_cuandoProveedorNoTieneHorarioPrevio() {
        autenticarProveedorConPerfil();
        BloqueHorario nuevo = bloque(DiaSemana.SABADO, "10:00", "14:00");
        when(horarioValidator.validar(request)).thenReturn(new ArrayList<>(List.of(nuevo)));
        when(horarioRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.empty());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfil));
        when(horarioRepository.save(any(HorarioProveedor.class))).thenAnswer(inv -> inv.getArgument(0));
        ArgumentCaptor<HorarioProveedor> captor = ArgumentCaptor.forClass(HorarioProveedor.class);

        HorarioSemanalResponse response = horarioService.guardarMiHorario(request);

        verify(horarioRepository).save(captor.capture());
        HorarioProveedor guardado = captor.getValue();
        assertSame(perfil, guardado.getProveedor());
        assertNull(guardado.getIdHorario());
        assertEquals(OffsetDateTime.now(clock), guardado.getFechaActualizacion());
        assertEquals(List.of(nuevo), guardado.getBloques());
        assertEquals(HorarioService.MSG_GUARDADO, response.getMensaje());
        assertEquals(List.of(new BloqueHorarioDTO("10:00", "14:00")),
                diaRespuesta(response, DiaSemana.SABADO).getBloques());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoPerfilDesapareceAlCrearHorario() {
        autenticarProveedorConPerfil();
        when(horarioValidator.validar(request)).thenReturn(List.of(bloque(DiaSemana.LUNES, "08:00", "12:00")));
        when(horarioRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.empty());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> horarioService.guardarMiHorario(request));

        assertEquals(HorarioService.MSG_SIN_PERFIL, ex.getMessage());
        verify(horarioRepository, never()).save(any(HorarioProveedor.class));
    }
}
