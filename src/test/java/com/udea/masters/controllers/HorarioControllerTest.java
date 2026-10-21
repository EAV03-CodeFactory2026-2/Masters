package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.BloqueHorarioDTO;
import com.udea.masters.controllers.DTOs.DiaHorarioRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalRequest;
import com.udea.masters.controllers.DTOs.HorarioSemanalResponse;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.HorarioInvalidoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.services.HorarioService;
import com.udea.masters.services.HorarioValidator;
import com.udea.masters.services.IHorarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioControllerTest {

    @Mock
    private IHorarioService horarioService;

    private HorarioController controller;

    @BeforeEach
    void setUp() {
        controller = new HorarioController(horarioService);
    }

    @Test
    void debeRetornar200ConHorario_cuandoConsulta() {
        HorarioSemanalResponse esperado = HorarioSemanalResponse.desde(null, null);
        when(horarioService.consultarMiHorario()).thenReturn(esperado);

        ResponseEntity<HorarioSemanalResponse> response = controller.consultar();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
        assertEquals(7, response.getBody().getDias().size());
    }

    @Test
    void debePropagarAccesoDenegado_cuandoConsultaSinRolProveedor() {
        when(horarioService.consultarMiHorario())
                .thenThrow(new AccesoDenegadoException(HorarioService.MSG_SOLO_PROVEEDOR));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> controller.consultar());

        assertEquals(HorarioService.MSG_SOLO_PROVEEDOR, ex.getMessage());
    }

    @Test
    void debeRetornar200ConMensaje_cuandoGuardaHorario() {
        HorarioSemanalRequest request = HorarioSemanalRequest.builder()
                .dias(List.of(new DiaHorarioRequest("LUNES", true, List.of(new BloqueHorarioDTO("08:00", "12:00")))))
                .build();
        HorarioSemanalResponse esperado = HorarioSemanalResponse.builder()
                .mensaje(HorarioService.MSG_GUARDADO)
                .fechaActualizacion(OffsetDateTime.parse("2026-03-10T15:30:00Z"))
                .dias(List.of())
                .build();
        when(horarioService.guardarMiHorario(request)).thenReturn(esperado);

        ResponseEntity<HorarioSemanalResponse> response = controller.guardar(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
        assertEquals(HorarioService.MSG_GUARDADO, response.getBody().getMensaje());
        verify(horarioService).guardarMiHorario(request);
    }

    @Test
    void debeDelegarRequestNull_cuandoGuardaSinCuerpo() {
        when(horarioService.guardarMiHorario(null))
                .thenThrow(new HorarioInvalidoException(HorarioValidator.MSG_SIN_DIAS_ACTIVOS));

        HorarioInvalidoException ex = assertThrows(HorarioInvalidoException.class, () -> controller.guardar(null));

        assertEquals(HorarioValidator.MSG_SIN_DIAS_ACTIVOS, ex.getMessage());
        verify(horarioService).guardarMiHorario(null);
    }

    @Test
    void debePropagarRecursoNoEncontrado_cuandoGuardaSinPerfil() {
        HorarioSemanalRequest request = new HorarioSemanalRequest(List.of());
        when(horarioService.guardarMiHorario(request))
                .thenThrow(new RecursoNoEncontradoException(HorarioService.MSG_SIN_PERFIL));

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> controller.guardar(request));

        assertEquals(HorarioService.MSG_SIN_PERFIL, ex.getMessage());
    }
}
