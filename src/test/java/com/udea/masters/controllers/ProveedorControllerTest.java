package com.udea.masters.controllers;

import com.udea.masters.controllers.DTOs.DirectorioProveedoresResponse;
import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.services.IProveedorService;
import com.udea.masters.services.ProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private IProveedorService proveedorService;

    private ProveedorController controller;

    @BeforeEach
    void setUp() {
        controller = new ProveedorController(proveedorService);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Carlos, null,    null, null, Carlos, null",
            "Carlos, Ana,     null, null, Carlos, null",
            "null,   Ana,     null, null, Ana,    null",
            "'   ',  Ana,     null, null, Ana,    null",
            "'',     Ana,     null, null, Ana,    null",
            "'   ',  null,    null, null, null,   null",
            "null,   null,    null, null, null,   null"
    })
    void debeResolverTextoDeBusqueda_cuandoLlegaQueryOCriterio(String query, String criterio, Integer pagina,
                                                               Integer page, String textoEsperado,
                                                               Integer paginaEsperada) {
        DirectorioProveedoresResponse esperado = DirectorioProveedoresResponse.builder().pagina(1).build();
        when(proveedorService.consultarDirectorio(textoEsperado, paginaEsperada)).thenReturn(esperado);

        ResponseEntity<DirectorioProveedoresResponse> response =
                controller.consultarDirectorio(query, criterio, pagina, page);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
        verify(proveedorService).consultarDirectorio(textoEsperado, paginaEsperada);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "2,    null, 2",
            "2,    5,    2",
            "0,    3,    0",
            "-4,   null, -4",
            "null, null, null",
            "null, 0,    1",
            "null, 3,    4",
            "null, -1,   -1",
            "null, -7,   -7"
    })
    void debeResolverNumeroDePagina_cuandoLleganPaginaOPage(Integer pagina, Integer page, Integer paginaEsperada) {
        DirectorioProveedoresResponse esperado = DirectorioProveedoresResponse.builder().build();
        when(proveedorService.consultarDirectorio("Car", paginaEsperada)).thenReturn(esperado);

        ResponseEntity<DirectorioProveedoresResponse> response =
                controller.consultarDirectorio("Car", null, pagina, page);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(esperado, response.getBody());
    }

    @Test
    void debeRetornar200ConProveedores_cuandoServicioEncuentraResultados() {
        ProveedorDirectorioResponse proveedor = ProveedorDirectorioResponse.builder()
                .idUsuario(5)
                .nombre("Carlos")
                .apellido("Arango")
                .tipoDocumento("CC")
                .numeroDocumento("1001")
                .esCuentaActual(true)
                .build();
        DirectorioProveedoresResponse esperado = DirectorioProveedoresResponse.builder()
                .mensaje(ProveedorService.MSG_CON_RESULTADOS)
                .proveedores(List.of(proveedor))
                .pagina(1)
                .tamanoPagina(10)
                .totalResultados(1)
                .totalPaginas(1)
                .hayMasPaginas(false)
                .build();
        when(proveedorService.consultarDirectorio("Carlos", 1)).thenReturn(esperado);

        ResponseEntity<DirectorioProveedoresResponse> response =
                controller.consultarDirectorio(null, "Carlos", null, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DirectorioProveedoresResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ProveedorService.MSG_CON_RESULTADOS, body.getMensaje());
        assertEquals(1, body.getTotalResultados());
        assertEquals("Carlos", body.getProveedores().get(0).getNombre());
        assertTrue(body.getProveedores().get(0).isEsCuentaActual());
    }

    @Test
    void debePropagarReglaNegocio_cuandoCriterioEsInvalido() {
        when(proveedorService.consultarDirectorio("ab", null))
                .thenThrow(new ReglaNegocioException(ProveedorService.MSG_MIN_CARACTERES));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> controller.consultarDirectorio("ab", null, null, null));

        assertEquals(ProveedorService.MSG_MIN_CARACTERES, ex.getMessage());
    }

    @Test
    void debePropagarReglaNegocio_cuandoPageEsNegativo() {
        when(proveedorService.consultarDirectorio("Carlos", -1))
                .thenThrow(new ReglaNegocioException(ProveedorService.MSG_PAGINA_INVALIDA));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> controller.consultarDirectorio("Carlos", null, null, -1));

        assertEquals(ProveedorService.MSG_PAGINA_INVALIDA, ex.getMessage());
    }
}
