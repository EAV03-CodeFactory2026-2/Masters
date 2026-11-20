package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.CrearFranjaRequest;
import com.udea.masters.controllers.DTOs.FranjaServicioResponse;
import com.udea.masters.entities.BloqueHorario;
import com.udea.masters.entities.DiaSemana;
import com.udea.masters.entities.FranjaServicio;
import com.udea.masters.entities.HorarioProveedor;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.Servicio;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.FranjaServicioRepository;
import com.udea.masters.repositories.HorarioProveedorRepository;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.repositories.ServicioProveedorRepository;
import com.udea.masters.repositories.ServicioRepository;
import com.udea.masters.repositories.UsuarioRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranjaServicioServiceTest {

    private static final Integer SERVICIO_ID = 10;
    private static final Integer NEGOCIO_ID = 20;
    private static final Integer PROVEEDOR_ID = 7;
    private static final Integer PROPIETARIO_ID = 5;
    private static final Integer FRANJA_ID = 100;
    private static final String ROL_PROVEEDOR = "Proveedor";
    private static final String ROL_PROPIETARIO = "Propietario";
    private static final Instant INSTANTE_FIJO = Instant.parse("2026-11-09T13:00:00Z");

    @Mock
    private FranjaServicioRepository franjaServicioRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private HorarioProveedorRepository horarioProveedorRepository;

    @Mock
    private ServicioProveedorRepository servicioProveedorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    private final Clock clock = Clock.fixed(INSTANTE_FIJO, ZoneOffset.UTC);

    private FranjaServicioService franjaServicioService;
    private Negocio negocio;
    private Servicio servicioCorte;
    private Usuario usuarioProveedor;
    private Usuario usuarioPropietaria;
    private PerfilProveedor perfilProveedor;
    private HorarioProveedor horarioLunesManana;

    @BeforeEach
    void setUp() {
        franjaServicioService = new FranjaServicioService(
                franjaServicioRepository,
                servicioRepository,
                perfilProveedorRepository,
                horarioProveedorRepository,
                servicioProveedorRepository,
                usuarioRepository,
                authenticationFacade,
                clock
        );

        negocio = Negocio.builder()
                .idNegocio(NEGOCIO_ID)
                .propietarioId(PROPIETARIO_ID)
                .nombreNegocio("Barbería Centro")
                .build();

        servicioCorte = Servicio.builder()
                .idServicio(SERVICIO_ID)
                .negocio(negocio)
                .nombre("Corte clásico")
                .duracionMinutos(30)
                .build();

        usuarioProveedor = Usuario.builder()
                .idUsuario(PROVEEDOR_ID)
                .nombre("Carlos")
                .apellido("Ruiz")
                .build();

        usuarioPropietaria = Usuario.builder()
                .idUsuario(PROPIETARIO_ID)
                .nombre("Ana")
                .apellido("Gómez")
                .build();

        perfilProveedor = PerfilProveedor.builder()
                .usuarioId(PROVEEDOR_ID)
                .usuario(usuarioProveedor)
                .especialidad("Barbería")
                .build();

        horarioLunesManana = HorarioProveedor.builder()
                .proveedor(perfilProveedor)
                .bloques(List.of(BloqueHorario.builder()
                        .diaSemana(DiaSemana.LUNES)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .build()))
                .build();
    }

    private void autenticarComoProveedor(Integer usuarioId) {
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(usuarioId);
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(true);
        when(authenticationFacade.tieneRol(ROL_PROPIETARIO)).thenReturn(false);
    }

    private void autenticarComoPropietario(Integer usuarioId) {
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(usuarioId);
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(false);
        when(authenticationFacade.tieneRol(ROL_PROPIETARIO)).thenReturn(true);
    }

    @Test
    void debeCrearFranja_cuandoProveedorAsignadoDefineHorarioValido() {
        // HU-0046/CA-01
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(usuarioProveedor));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> {
            FranjaServicio f = inv.getArgument(0);
            f.setIdFranja(FRANJA_ID);
            return f;
        });

        FranjaServicioResponse response = franjaServicioService.crearFranja(SERVICIO_ID, request);

        assertEquals(FranjaServicioService.MSG_FRANJA_CREADA, response.getMensaje());
        assertEquals(FRANJA_ID, response.getIdFranja());
        assertEquals(SERVICIO_ID, response.getServicioId());
        assertEquals(PROVEEDOR_ID, response.getProveedorId());
        assertEquals("Carlos Ruiz", response.getNombreProveedor());
        assertEquals("LUNES", response.getDiaSemana());
        assertEquals("09:00", response.getHoraInicio());
        assertEquals("09:30", response.getHoraFin());
        assertEquals(PROVEEDOR_ID, response.getCreadaPorId());
        assertEquals("Carlos Ruiz", response.getNombreCreadaPor());
        assertEquals(OffsetDateTime.now(clock), response.getFechaCreacion());
    }

    @Test
    void debeCrearFranjaConServicioIdDelBody_cuandoParametroRutaEsNulo() {
        // HU-0046/CA-01
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .servicioId(SERVICIO_ID)
                .proveedorId(PROVEEDOR_ID)
                .diaSemana("miércoles")
                .horaInicio("09:00")
                .build();
        HorarioProveedor horarioMiercoles = HorarioProveedor.builder()
                .proveedor(perfilProveedor)
                .bloques(List.of(BloqueHorario.builder()
                        .diaSemana(DiaSemana.MIERCOLES)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .build()))
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioMiercoles));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.MIERCOLES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(usuarioProveedor));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        FranjaServicioResponse response = franjaServicioService.crearFranja(null, request);

        assertEquals("MIERCOLES", response.getDiaSemana());
        assertEquals("09:00", response.getHoraInicio());
        assertEquals("09:30", response.getHoraFin());
    }

    @Test
    void debeCrearFranjaYRegistrarAutor_cuandoPropietarioDelNegocioDefineFranjaParaProveedorAsignado() {
        // HU-0046/CA-02
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .proveedorId(PROVEEDOR_ID)
                .diaSemana("LUNES")
                .horaInicio("10:30")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoPropietario(PROPIETARIO_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROPIETARIO_ID)).thenReturn(Optional.of(usuarioPropietaria));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> {
            FranjaServicio f = inv.getArgument(0);
            f.setIdFranja(FRANJA_ID);
            return f;
        });

        FranjaServicioResponse response = franjaServicioService.crearFranja(SERVICIO_ID, request);

        ArgumentCaptor<FranjaServicio> captor = ArgumentCaptor.forClass(FranjaServicio.class);
        verify(franjaServicioRepository).save(captor.capture());
        FranjaServicio guardada = captor.getValue();

        assertEquals(usuarioPropietaria, guardada.getCreadaPor());
        assertEquals(OffsetDateTime.now(clock), guardada.getFechaCreacion());
        assertEquals("10:30", response.getHoraInicio());
        assertEquals("11:00", response.getHoraFin());
        assertEquals(PROPIETARIO_ID, response.getCreadaPorId());
        assertEquals("Ana Gómez", response.getNombreCreadaPor());
        assertEquals("Carlos Ruiz", response.getNombreProveedor());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoFranjaExcedeCierreDelBloque() {
        // HU-0046/CA-03
        Servicio servicioBarbaYCorte = Servicio.builder()
                .idServicio(SERVICIO_ID)
                .negocio(negocio)
                .nombre("Barba y corte")
                .duracionMinutos(45)
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("11:30")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioBarbaYCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
        verify(franjaServicioRepository, never()).save(any());
    }

    @Test
    void debeAceptarFranja_cuandoTerminaExactamenteAlCierreDelBloque() {
        // HU-0046/CA-03
        Servicio servicioBarbaYCorte = Servicio.builder()
                .idServicio(SERVICIO_ID)
                .negocio(negocio)
                .nombre("Barba y corte")
                .duracionMinutos(45)
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("11:15")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioBarbaYCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(usuarioProveedor));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        FranjaServicioResponse response = franjaServicioService.crearFranja(SERVICIO_ID, request);

        assertEquals("11:15", response.getHoraInicio());
        assertEquals("12:00", response.getHoraFin());
        verify(franjaServicioRepository).save(any(FranjaServicio.class));
    }

    @Test
    void debeLanzarReglaNegocio_cuandoFranjaIniciaAntesDelBloque() {
        // HU-0046/CA-03
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("07:30")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoProveedorNoTieneHorarioRegistrado() {
        // HU-0046/CA-03
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoHorarioNoTieneBloques() {
        // HU-0046/CA-03
        HorarioProveedor horarioVacio = HorarioProveedor.builder()
                .proveedor(perfilProveedor)
                .bloques(Collections.emptyList())
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioVacio));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoListaDeBloquesDelHorarioEsNula() {
        // HU-0046/CA-03
        HorarioProveedor horarioConBloquesNulos = HorarioProveedor.builder()
                .proveedor(perfilProveedor)
                .bloques(null)
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioConBloquesNulos));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoProveedorSoloAtiendeOtroDia() {
        // HU-0046/CA-03
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("MARTES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_HORARIO_NO_CABE, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoFranjaCruzaMedianoche() {
        // HU-0046/CA-03
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("23:45")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals("La franja no puede exceder el final del día.", ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoFranjaSeSuperponeConOtraExistente() {
        // HU-0046/CA-04
        FranjaServicio existente = FranjaServicio.builder()
                .idFranja(1)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:15")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(List.of(existente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_FRANJA_SUPERPUESTA, ex.getMessage());
        verify(franjaServicioRepository, never()).save(any());
    }

    @Test
    void debePermitirFranjaContigua_cuandoIniciaJustoAlTerminarOtra() {
        // HU-0046/CA-04
        FranjaServicio existente = FranjaServicio.builder()
                .idFranja(1)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:30")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(List.of(existente));
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(usuarioProveedor));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        FranjaServicioResponse response = franjaServicioService.crearFranja(SERVICIO_ID, request);

        assertEquals("09:30", response.getHoraInicio());
        assertEquals("10:00", response.getHoraFin());
        verify(franjaServicioRepository).save(any(FranjaServicio.class));
    }

    @Test
    void debePermitirFranjaContigua_cuandoTerminaJustoAlIniciarOtra() {
        // HU-0046/CA-04
        FranjaServicio existente = FranjaServicio.builder()
                .idFranja(1)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("08:30")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(List.of(existente));
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(usuarioProveedor));
        when(franjaServicioRepository.save(any(FranjaServicio.class))).thenAnswer(inv -> inv.getArgument(0));

        FranjaServicioResponse response = franjaServicioService.crearFranja(SERVICIO_ID, request);

        assertEquals("08:30", response.getHoraInicio());
        assertEquals("09:00", response.getHoraFin());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoProveedorNoTieneServicioAsignado() {
        // HU-0046/CA-05
        Integer otroProveedorId = 99;
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(otroProveedorId);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, otroProveedorId)).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO, ex.getMessage());
        verify(franjaServicioRepository, never()).save(any());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoPropietarioEsDeOtroNegocio() {
        // HU-0046/CA-05
        Integer otroPropietarioId = 88;
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .proveedorId(PROVEEDOR_ID)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoPropietario(otroPropietarioId);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO, ex.getMessage());
        verify(franjaServicioRepository, never()).save(any());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoProveedorIntentaCrearParaOtroProveedor() {
        // HU-0046/CA-05
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .proveedorId(99)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO, ex.getMessage());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoUsuarioNoEstaAutenticadoAlCrear() {
        // HU-0046/CA-05
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO, ex.getMessage());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoRolNoEsProveedorNiPropietario() {
        // HU-0046/CA-05
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(33);
        when(authenticationFacade.tieneRol(ROL_PROVEEDOR)).thenReturn(false);
        when(authenticationFacade.tieneRol(ROL_PROPIETARIO)).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoPropietarioNoIndicaProveedor() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .proveedorId(null)
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoPropietario(PROPIETARIO_ID);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_PROVEEDOR_OBLIGATORIO, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoRequestEsNulo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, null));

        assertEquals("La solicitud no puede estar vacía.", ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoServicioIdEsNuloEnRutaYEnBody() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(null, request));

        assertEquals("El identificador del servicio es obligatorio.", ex.getMessage());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoServicioNoExisteAlCrear() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoDiaSemanaEsNuloOVacio() {
        CrearFranjaRequest requestNulo = CrearFranjaRequest.builder()
                .diaSemana(null)
                .horaInicio("09:00")
                .build();
        CrearFranjaRequest requestVacio = CrearFranjaRequest.builder()
                .diaSemana("   ")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);

        ReglaNegocioException exNulo = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, requestNulo));
        ReglaNegocioException exVacio = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, requestVacio));

        assertEquals(FranjaServicioService.MSG_DIA_OBLIGATORIO, exNulo.getMessage());
        assertEquals(FranjaServicioService.MSG_DIA_OBLIGATORIO, exVacio.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoDiaSemanaNoEsValido() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("FESTIVO")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals("El día 'FESTIVO' no es válido.", ex.getMessage());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoHoraInicioEsNulaOFormatoInvalido() {
        CrearFranjaRequest requestNula = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio(null)
                .build();
        CrearFranjaRequest requestInvalida = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("25:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);

        ReglaNegocioException exNula = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, requestNula));
        ReglaNegocioException exInvalida = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, requestInvalida));

        assertEquals(FranjaServicioService.MSG_HORA_OBLIGATORIA, exNula.getMessage());
        assertEquals(FranjaServicioService.MSG_HORA_OBLIGATORIA, exInvalida.getMessage());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoPerfilProveedorNoExisteAlGuardar() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals(FranjaServicioService.MSG_PROVEEDOR_NO_ENCONTRADO, ex.getMessage());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoUsuarioCreadorNoExisteAlGuardar() {
        CrearFranjaRequest request = CrearFranjaRequest.builder()
                .diaSemana("LUNES")
                .horaInicio("09:00")
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicioCorte));
        autenticarComoProveedor(PROVEEDOR_ID);
        when(servicioProveedorRepository.existsByServicioIdAndProveedorId(SERVICIO_ID, PROVEEDOR_ID)).thenReturn(true);
        when(horarioProveedorRepository.findByProveedorUsuarioId(PROVEEDOR_ID)).thenReturn(Optional.of(horarioLunesManana));
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(perfilProveedor));
        when(usuarioRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> franjaServicioService.crearFranja(SERVICIO_ID, request));

        assertEquals("Usuario no encontrado.", ex.getMessage());
    }

    @Test
    void debeEliminarSoloLaFranja_cuandoProveedorTitularElimina() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        autenticarComoProveedor(PROVEEDOR_ID);

        franjaServicioService.eliminarFranja(SERVICIO_ID, FRANJA_ID);

        verify(franjaServicioRepository).delete(franja);
        verifyNoMoreInteractions(franjaServicioRepository);
        verifyNoInteractions(servicioRepository, perfilProveedorRepository, horarioProveedorRepository,
                servicioProveedorRepository, usuarioRepository);
    }

    @Test
    void debeEliminarSoloLaFranja_cuandoPropietarioDelNegocioElimina() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        autenticarComoPropietario(PROPIETARIO_ID);

        franjaServicioService.eliminarFranja(null, FRANJA_ID);

        verify(franjaServicioRepository).delete(franja);
        verifyNoMoreInteractions(franjaServicioRepository);
        verifyNoInteractions(servicioRepository, perfilProveedorRepository, horarioProveedorRepository,
                servicioProveedorRepository, usuarioRepository);
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoOtroProveedorIntentaEliminar() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        autenticarComoProveedor(99);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.eliminarFranja(SERVICIO_ID, FRANJA_ID));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO_MODIFICAR, ex.getMessage());
        verify(franjaServicioRepository, never()).delete(any());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoPropietarioDeOtroNegocioIntentaEliminar() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        autenticarComoPropietario(88);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.eliminarFranja(SERVICIO_ID, FRANJA_ID));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO_MODIFICAR, ex.getMessage());
        verify(franjaServicioRepository, never()).delete(any());
    }

    @Test
    void debeLanzarAccesoDenegado_cuandoEliminaSinUsuarioAutenticado() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> franjaServicioService.eliminarFranja(SERVICIO_ID, FRANJA_ID));

        assertEquals(FranjaServicioService.MSG_SIN_PERMISO_MODIFICAR, ex.getMessage());
        verify(franjaServicioRepository, never()).delete(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoFranjaNoPerteneceAlServicioAlEliminar() {
        // HU-0046/CA-06
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .build();
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.eliminarFranja(999, FRANJA_ID));

        assertEquals("La franja no pertenece al servicio indicado.", ex.getMessage());
        verify(franjaServicioRepository, never()).delete(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoIdFranjaEsNuloAlEliminar() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.eliminarFranja(SERVICIO_ID, null));

        assertEquals("El identificador de la franja es obligatorio.", ex.getMessage());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoFranjaNoExisteAlEliminar() {
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> franjaServicioService.eliminarFranja(SERVICIO_ID, FRANJA_ID));

        assertEquals(FranjaServicioService.MSG_FRANJA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    void debeConsultarFranjasPorServicioProveedorYDia() {
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicioCorte)
                .proveedor(perfilProveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        when(servicioRepository.existsById(SERVICIO_ID)).thenReturn(true);
        when(franjaServicioRepository.findByServicioAndProveedorAndDiaSemana(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES))
                .thenReturn(List.of(franja));

        List<FranjaServicioResponse> resultado = franjaServicioService.consultarFranjas(SERVICIO_ID, PROVEEDOR_ID, "lunes");

        assertEquals(1, resultado.size());
        assertNull(resultado.get(0).getMensaje());
        assertEquals(FRANJA_ID, resultado.get(0).getIdFranja());
        assertEquals("09:00", resultado.get(0).getHoraInicio());
        assertEquals("09:30", resultado.get(0).getHoraFin());
    }

    @Test
    void debeConsultarFranjasPorServicioYProveedor_cuandoDiaEsNuloOVacio() {
        when(servicioRepository.existsById(SERVICIO_ID)).thenReturn(true);
        when(franjaServicioRepository.findByServicioAndProveedor(SERVICIO_ID, PROVEEDOR_ID))
                .thenReturn(Collections.emptyList());

        List<FranjaServicioResponse> resNulo = franjaServicioService.consultarFranjas(SERVICIO_ID, PROVEEDOR_ID, null);
        List<FranjaServicioResponse> resVacio = franjaServicioService.consultarFranjas(SERVICIO_ID, PROVEEDOR_ID, "   ");

        assertTrue(resNulo.isEmpty());
        assertTrue(resVacio.isEmpty());
        verify(franjaServicioRepository, times(2)).findByServicioAndProveedor(SERVICIO_ID, PROVEEDOR_ID);
    }

    @Test
    void debeConsultarFranjasPorServicioYDia_cuandoProveedorEsNulo() {
        when(servicioRepository.existsById(SERVICIO_ID)).thenReturn(true);
        when(franjaServicioRepository.findByServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(Collections.emptyList());

        List<FranjaServicioResponse> resultado = franjaServicioService.consultarFranjas(SERVICIO_ID, null, "LUNES");

        assertTrue(resultado.isEmpty());
        verify(franjaServicioRepository).findByServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES);
    }

    @Test
    void debeConsultarFranjasSoloPorServicio_cuandoProveedorYDiaSonNulos() {
        when(servicioRepository.existsById(SERVICIO_ID)).thenReturn(true);
        when(franjaServicioRepository.findByServicio(SERVICIO_ID))
                .thenReturn(Collections.emptyList());

        List<FranjaServicioResponse> resultado = franjaServicioService.consultarFranjas(SERVICIO_ID, null, null);

        assertTrue(resultado.isEmpty());
        verify(franjaServicioRepository).findByServicio(SERVICIO_ID);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoServicioIdEsNuloEnConsulta() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> franjaServicioService.consultarFranjas(null, PROVEEDOR_ID, "LUNES"));

        assertEquals("El identificador del servicio es obligatorio.", ex.getMessage());
    }

    @Test
    void debeLanzarRecursoNoEncontrado_cuandoServicioNoExisteEnConsulta() {
        when(servicioRepository.existsById(SERVICIO_ID)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> franjaServicioService.consultarFranjas(SERVICIO_ID, PROVEEDOR_ID, "LUNES"));

        assertEquals(FranjaServicioService.MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
    }
}
