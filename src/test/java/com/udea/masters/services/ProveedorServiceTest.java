package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.DirectorioProveedoresResponse;
import com.udea.masters.controllers.DTOs.ProveedorDirectorioResponse;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.security.IAuthenticationFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {

    private static final String ROL_PROVEEDOR = "Proveedor";

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    @InjectMocks
    private ProveedorService proveedorService;

    private static PerfilProveedor perfil(Integer id, String nombre, String apellido, String documento) {
        Usuario usuario = Usuario.builder()
                .idUsuario(id)
                .nombre(nombre)
                .apellido(apellido)
                .correo(nombre.toLowerCase() + "@correo.com")
                .telefono("300000000" + id)
                .build();
        return PerfilProveedor.builder()
                .usuarioId(id)
                .usuario(usuario)
                .tipoDocumento(TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build())
                .numeroDocumento(documento)
                .especialidad("Barbería")
                .descripcionProfesional("Cortes clásicos")
                .build();
    }

    private void stubBusqueda(String patron, String documento, Pageable pageable, Page<PerfilProveedor> pagina) {
        when(perfilProveedorRepository.buscarEnDirectorio(patron, documento, ROL_PROVEEDOR, pageable))
                .thenReturn(pagina);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "ab", "  a  ", " ab ", "\t a \n"})
    void debeLanzarReglaNegocio_cuandoCriterioTieneMenosDeTresCaracteres(String criterio) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> proveedorService.consultarDirectorio(criterio, 1));

        assertEquals(ProveedorService.MSG_MIN_CARACTERES, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository, authenticationFacade);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoCriterioSuperaCienCaracteres() {
        String criterio = "a".repeat(ProveedorService.MAX_CARACTERES + 1);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> proveedorService.consultarDirectorio(criterio, 1));

        assertEquals(ProveedorService.MSG_MAX_CARACTERES, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository, authenticationFacade);
    }

    @Test
    void debeAceptarCriterio_cuandoTieneCienCaracteresTrasColapsarEspacios() {
        String criterio = "   " + "a".repeat(50) + "      " + "b".repeat(49) + "   ";
        String limpio = "a".repeat(50) + " " + "b".repeat(49);
        stubBusqueda("%" + limpio + "%", limpio, PageRequest.of(0, 10), Page.empty());

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio(criterio, 1);

        assertEquals(ProveedorService.MSG_SIN_RESULTADOS, response.getMensaje());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -50})
    void debeLanzarReglaNegocio_cuandoPaginaMenorAUno(int pagina) {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> proveedorService.consultarDirectorio("Carlos", pagina));

        assertEquals(ProveedorService.MSG_PAGINA_INVALIDA, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository, authenticationFacade);
    }

    @Test
    void debeConsultarPrimeraPagina_cuandoPaginaEsNull() {
        stubBusqueda("%car%", "car", PageRequest.of(0, ProveedorService.TAMANO_PAGINA), Page.empty());

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio("Car", null);

        assertEquals(1, response.getPagina());
        assertEquals(ProveedorService.TAMANO_PAGINA, response.getTamanoPagina());
    }

    @Test
    void debeColapsarEspaciosYPasarAMinusculas_cuandoCriterioTieneEspaciosInternos() {
        stubBusqueda("%a b%", "a b", PageRequest.of(0, 10), Page.empty());

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio("  A   b ", 1);

        assertEquals(ProveedorService.MSG_SIN_RESULTADOS, response.getMensaje());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "50%off|%50!%off%|50%off",
            "mi_barbero|%mi!_barbero%|mi_barbero",
            "hola!|%hola!!%|hola!",
            "!%_X|%!!!%!_x%|!%_x"
    })
    void debeEscaparComodinesLike_cuandoCriterioLosContiene(String criterio, String patron, String documento) {
        when(perfilProveedorRepository.buscarEnDirectorio(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());

        proveedorService.consultarDirectorio(criterio, 1);

        verify(perfilProveedorRepository).buscarEnDirectorio(eq(patron), eq(documento), eq(ROL_PROVEEDOR),
                eq(PageRequest.of(0, 10)));
    }

    @Test
    void debeRetornarProveedores_cuandoHayResultados() {
        PerfilProveedor actual = perfil(5, "Carlos", "Arango", "1001");
        PerfilProveedor otro = perfil(8, "Carla", "Botero", "1002");
        Pageable pageable = PageRequest.of(1, 10);
        stubBusqueda("%carl%", "carl", pageable, new PageImpl<>(List.of(actual, otro), pageable, 25));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(5);

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio("Carl", 2);

        assertEquals(ProveedorService.MSG_CON_RESULTADOS, response.getMensaje());
        assertEquals(2, response.getPagina());
        assertEquals(10, response.getTamanoPagina());
        assertEquals(25, response.getTotalResultados());
        assertEquals(3, response.getTotalPaginas());
        assertTrue(response.isHayMasPaginas());
        assertEquals(2, response.getProveedores().size());

        ProveedorDirectorioResponse primero = response.getProveedores().get(0);
        assertEquals(5, primero.getIdUsuario());
        assertEquals("Carlos", primero.getNombre());
        assertEquals("Arango", primero.getApellido());
        assertEquals("carlos@correo.com", primero.getCorreo());
        assertEquals("3000000005", primero.getTelefono());
        assertEquals("CC", primero.getTipoDocumento());
        assertEquals("Cédula de ciudadanía", primero.getTipoDocumentoNombre());
        assertEquals("1001", primero.getNumeroDocumento());
        assertEquals("Barbería", primero.getEspecialidad());
        assertEquals("Cortes clásicos", primero.getDescripcionProfesional());
        assertTrue(primero.isEsCuentaActual());

        ProveedorDirectorioResponse segundo = response.getProveedores().get(1);
        assertEquals(8, segundo.getIdUsuario());
        assertFalse(segundo.isEsCuentaActual());
    }

    @Test
    void debeMarcarNingunaCuentaActual_cuandoNoHayUsuarioAutenticado() {
        Pageable pageable = PageRequest.of(0, 10);
        stubBusqueda("%1001%", "1001", pageable, new PageImpl<>(List.of(perfil(5, "Carlos", "Arango", "1001")), pageable, 1));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio("1001", 1);

        assertEquals(1, response.getProveedores().size());
        assertFalse(response.getProveedores().get(0).isEsCuentaActual());
        assertFalse(response.isHayMasPaginas());
        assertEquals(1, response.getTotalPaginas());
    }

    @Test
    void debeRetornarMensajeSinResultados_cuandoNoHayCoincidencias() {
        Pageable pageable = PageRequest.of(0, 10);
        stubBusqueda("%zzz%", "zzz", pageable, new PageImpl<>(List.of(), pageable, 0));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(5);

        DirectorioProveedoresResponse response = proveedorService.consultarDirectorio("ZZZ", 1);

        assertEquals(ProveedorService.MSG_SIN_RESULTADOS, response.getMensaje());
        assertTrue(response.getProveedores().isEmpty());
        assertEquals(0, response.getTotalResultados());
        assertEquals(0, response.getTotalPaginas());
        assertFalse(response.isHayMasPaginas());
        assertEquals(1, response.getPagina());
    }
}
