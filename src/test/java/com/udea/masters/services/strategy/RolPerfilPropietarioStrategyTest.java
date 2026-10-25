package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.PerfilPropietario;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilPropietarioRepository;
import com.udea.masters.repositories.PerfilProveedorRepository;
import com.udea.masters.repositories.TipoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolPerfilPropietarioStrategyTest {

    @Mock
    private PerfilPropietarioRepository perfilPropietarioRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @InjectMocks
    private RolPerfilPropietarioStrategy strategy;

    private Usuario usuario;
    private TipoDocumento cedula;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(12).build();
        cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
    }

    private RolAsignacionRequest request(String tipoDocumento, String numeroDocumento) {
        return RolAsignacionRequest.builder()
                .nombreRol("Propietario")
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(numeroDocumento)
                .build();
    }

    private PerfilPropietario capturarPerfilGuardado() {
        ArgumentCaptor<PerfilPropietario> captor = ArgumentCaptor.forClass(PerfilPropietario.class);
        verify(perfilPropietarioRepository).save(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Propietario", "propietario", "PROPIETARIO"})
    void debeRetornarVerdadero_cuandoRolEsPropietario(String rol) {
        assertTrue(strategy.soporta(rol));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Propietario", "propietario", "PROPIETARIO", " Propietario ", "  PROPIETARIO  "})
    void debeRetornarVerdadero_cuandoRolEsPropietarioConEspacios(String rol) {
        // H-04: trim() unificado — los espacios alrededor ya no impiden la coincidencia
        assertTrue(strategy.soporta(rol));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Cliente", "Proveedor"})
    void debeRetornarFalso_cuandoRolNoEsPropietario(String rol) {
        assertFalse(strategy.soporta(rol));
    }

    @Test
    void debeRetornarPropietario_cuandoSeConsultaNombreRol() {
        assertEquals("Propietario", strategy.getNombreRol());
    }

    @Test
    void debeGuardarPerfil_cuandoRequestTraeDocumentoCompleto() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "1234567")).thenReturn(false);

        strategy.asignarPerfil(usuario, request(" CC ", " 1234567 "));

        PerfilPropietario perfil = capturarPerfilGuardado();
        assertSame(usuario, perfil.getUsuario());
        assertSame(cedula, perfil.getTipoDocumento());
        assertEquals("1234567", perfil.getNumeroDocumento());
        verifyNoInteractions(perfilProveedorRepository);
    }

    // H-02: Solo se reutiliza el perfil Proveedor cuando AMBOS campos vienen vacíos
    @ParameterizedTest
    @CsvSource(value = {
            "NULL, NULL",
            "'  ', ''"
    }, nullValues = "NULL")
    void debeReutilizarDocumentoDelProveedor_cuandoAmbosCamposVaciosONulos(String tipoDocumento, String numeroDocumento) {
        PerfilProveedor proveedor = PerfilProveedor.builder().usuarioId(12).tipoDocumento(cedula).numeroDocumento("555").build();
        when(perfilProveedorRepository.findById(12)).thenReturn(Optional.of(proveedor));
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "555")).thenReturn(false);

        strategy.asignarPerfil(usuario, request(tipoDocumento, numeroDocumento));

        PerfilPropietario perfil = capturarPerfilGuardado();
        assertSame(cedula, perfil.getTipoDocumento());
        assertEquals("555", perfil.getNumeroDocumento());
    }

    // H-02: Si solo llega el tipo, se exige el número en lugar de mezclarlo con el perfil Proveedor
    @Test
    void debeLanzarReglaNegocio_cuandoSoloLlegaTipoSinNumero() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request("CC", null)));

        assertEquals("Número de documento es obligatorio", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
        verifyNoInteractions(perfilProveedorRepository);
    }

    // H-02: Si solo llega el número, se exige el tipo en lugar de mezclarlo con el perfil Proveedor
    @Test
    void debeLanzarReglaNegocio_cuandoSoloLlegaNumeroSinTipo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request(null, "555")));

        assertEquals("Tipo de documento es obligatorio", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
        verifyNoInteractions(perfilProveedorRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoProveedorNoTieneTipoDocumento() {
        PerfilProveedor proveedor = PerfilProveedor.builder().usuarioId(12).numeroDocumento("555").build();
        when(perfilProveedorRepository.findById(12)).thenReturn(Optional.of(proveedor));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request(null, null)));

        assertEquals("Tipo de documento es obligatorio", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoProveedorNoTieneNumeroDocumento() {
        PerfilProveedor proveedor = PerfilProveedor.builder().usuarioId(12).tipoDocumento(cedula).build();
        when(perfilProveedorRepository.findById(12)).thenReturn(Optional.of(proveedor));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request(null, null)));

        assertEquals("Número de documento es obligatorio", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, NULL, Tipo de documento es obligatorio",
            "'', '', Tipo de documento es obligatorio"
    }, nullValues = "NULL")
    void debeLanzarReglaNegocio_cuandoFaltanDatosYNoHayPerfilProveedor(String tipoDocumento, String numeroDocumento, String mensaje) {
        when(perfilProveedorRepository.findById(12)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request(tipoDocumento, numeroDocumento)));

        assertEquals(mensaje, ex.getMessage());
        verifyNoInteractions(tipoDocumentoRepository);
        verify(perfilPropietarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExiste() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("XX")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> strategy.asignarPerfil(usuario, request(" XX ", "555")));

        assertEquals("Tipo de documento no válido: XX", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
    }

    @Test
    void debeLanzarRecursoDuplicado_cuandoDocumentoYaRegistrado() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "555")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> strategy.asignarPerfil(usuario, request("CC", "555")));

        assertEquals("Ya existe un propietario registrado con este documento.", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
    }

    @Test
    void debeEliminarPerfilPropietario_cuandoSeRemueveRol() {
        strategy.removerPerfil(12);

        verify(perfilPropietarioRepository).deleteById(12);
    }
}
