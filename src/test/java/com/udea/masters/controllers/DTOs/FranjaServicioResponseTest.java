package com.udea.masters.controllers.DTOs;

import com.udea.masters.entities.DiaSemana;
import com.udea.masters.entities.FranjaServicio;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.Servicio;
import com.udea.masters.entities.Usuario;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class FranjaServicioResponseTest {

    @Test
    void debeRetornarNull_cuandoFranjaEsNula() {
        FranjaServicioResponse response = FranjaServicioResponse.desde(null, "Mensaje");

        assertNull(response);
    }

    @Test
    void debeMapearTodosLosCampos_cuandoFranjaEstaCompleta() {
        // HU-0046/CA-01
        OffsetDateTime fechaCreacion = OffsetDateTime.of(2026, 11, 9, 8, 0, 0, 0, ZoneOffset.UTC);
        Servicio servicio = Servicio.builder()
                .idServicio(10)
                .nombre("Corte clásico")
                .duracionMinutos(30)
                .build();
        Usuario usuarioProveedor = Usuario.builder()
                .idUsuario(7)
                .nombre("Carlos")
                .apellido("Ruiz")
                .build();
        PerfilProveedor proveedor = PerfilProveedor.builder()
                .usuarioId(7)
                .usuario(usuarioProveedor)
                .build();
        Usuario autora = Usuario.builder()
                .idUsuario(5)
                .nombre("Ana")
                .apellido("Gómez")
                .build();
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(100)
                .servicio(servicio)
                .proveedor(proveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .creadaPor(autora)
                .fechaCreacion(fechaCreacion)
                .build();

        FranjaServicioResponse response = FranjaServicioResponse.desde(franja, "Franja creada correctamente.");

        assertNotNull(response);
        assertEquals("Franja creada correctamente.", response.getMensaje());
        assertEquals(100, response.getIdFranja());
        assertEquals(10, response.getServicioId());
        assertEquals("Corte clásico", response.getNombreServicio());
        assertEquals(30, response.getDuracionMinutos());
        assertEquals(7, response.getProveedorId());
        assertEquals("Carlos Ruiz", response.getNombreProveedor());
        assertEquals("LUNES", response.getDiaSemana());
        assertEquals("09:00", response.getHoraInicio());
        assertEquals("09:30", response.getHoraFin());
        assertEquals(5, response.getCreadaPorId());
        assertEquals("Ana Gómez", response.getNombreCreadaPor());
        assertEquals(fechaCreacion, response.getFechaCreacion());
    }

    @Test
    void debeMapearCamposNulos_cuandoRelacionesDeFranjaSonNulas() {
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(101)
                .build();

        FranjaServicioResponse response = FranjaServicioResponse.desde(franja, null);

        assertNotNull(response);
        assertNull(response.getMensaje());
        assertEquals(101, response.getIdFranja());
        assertNull(response.getServicioId());
        assertNull(response.getNombreServicio());
        assertNull(response.getDuracionMinutos());
        assertNull(response.getProveedorId());
        assertNull(response.getNombreProveedor());
        assertNull(response.getDiaSemana());
        assertNull(response.getHoraInicio());
        assertNull(response.getHoraFin());
        assertNull(response.getCreadaPorId());
        assertNull(response.getNombreCreadaPor());
        assertNull(response.getFechaCreacion());
    }

    @Test
    void debeMapearSinUsuarioDeProveedorNiHoraFin_cuandoFaltanDatosAnidados() {
        Servicio servicioSinDuracion = Servicio.builder()
                .idServicio(15)
                .nombre("Masaje")
                .duracionMinutos(null)
                .build();
        PerfilProveedor proveedorSinUsuario = PerfilProveedor.builder()
                .usuarioId(12)
                .usuario(null)
                .build();
        FranjaServicio franja = FranjaServicio.builder()
                .idFranja(102)
                .servicio(servicioSinDuracion)
                .proveedor(proveedorSinUsuario)
                .horaInicio(LocalTime.of(10, 0))
                .build();

        FranjaServicioResponse response = FranjaServicioResponse.desde(franja, null);

        assertNotNull(response);
        assertEquals(15, response.getServicioId());
        assertEquals(12, response.getProveedorId());
        assertNull(response.getNombreProveedor());
        assertEquals("10:00", response.getHoraInicio());
        assertNull(response.getHoraFin());
    }
}
