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
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolPerfilProveedorStrategyTest {

    private static final String MENSAJE_ESPECIALIDAD = "La especialidad debe tener entre 2 y 100 caracteres.";

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private PerfilPropietarioRepository perfilPropietarioRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @InjectMocks
    private RolPerfilProveedorStrategy strategy;

    private Usuario usuario;
    private TipoDocumento cedula;
    private RolAsignacionRequest request;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(20).build();
        cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
        request = RolAsignacionRequest.builder()
                .nombreRol("Proveedor")
                .tipoDocumento(" CC ")
                .numeroDocumento(" 777888 ")
                .especialidad("  Manicure  ")
                .descripcionProfesional("  Uñas acrílicas  ")
                .build();
    }

    private PerfilProveedor capturarPerfilGuardado() {
        ArgumentCaptor<PerfilProveedor> captor = ArgumentCaptor.forClass(PerfilProveedor.class);
        verify(perfilProveedorRepository).save(captor.capture());
        return captor.getValue();
    }

    private void prepararDocumentoDisponible(String numero) {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, numero)).thenReturn(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Proveedor", "proveedor", "PROVEEDOR", " Proveedor ", "  PROVEEDOR  "})
    void debeRetornarVerdadero_cuandoRolEsProveedor(String rol) {
        // H-04: trim() unificado
        boolean resultado = strategy.soporta(rol);

        assertTrue(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Cliente", "Propietario", "Administrador"})
    void debeRetornarFalso_cuandoRolNoEsProveedor(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertFalse(resultado);
    }

    @Test
    void debeRetornarProveedor_cuandoSeConsultaNombreRol() {
        String nombre = strategy.getNombreRol();

        assertEquals("Proveedor", nombre);
    }

    @Test
    void debeGuardarPerfil_cuandoRequestTraeDatosCompletos() {
        prepararDocumentoDisponible("777888");

        strategy.asignarPerfil(usuario, request);

        PerfilProveedor perfil = capturarPerfilGuardado();
        assertSame(usuario, perfil.getUsuario());
        assertSame(cedula, perfil.getTipoDocumento());
        assertEquals("777888", perfil.getNumeroDocumento());
        assertEquals("Manicure", perfil.getEspecialidad());
        assertEquals("Uñas acrílicas", perfil.getDescripcionProfesional());
        verifyNoInteractions(perfilPropietarioRepository);
    }

    @Test
    void debeGuardarDescripcionNull_cuandoNoSeEnviaDescripcion() {
        request.setDescripcionProfesional(null);
        prepararDocumentoDisponible("777888");

        strategy.asignarPerfil(usuario, request);

        assertNull(capturarPerfilGuardado().getDescripcionProfesional());
    }

    // H-02: Solo se reutiliza cuando AMBOS campos vienen vacíos
    @ParameterizedTest
    @CsvSource(value = {
            "NULL, NULL",
            "' ', ''"
    }, nullValues = "NULL")
    void debeReutilizarDocumentoDelPropietario_cuandoAmbosCamposVacios(String tipoDocumento, String numeroDocumento) {
        request.setTipoDocumento(tipoDocumento);
        request.setNumeroDocumento(numeroDocumento);
        PerfilPropietario propietario = PerfilPropietario.builder().usuarioId(20).tipoDocumento(cedula).numeroDocumento("999").build();
        when(perfilPropietarioRepository.findById(20)).thenReturn(Optional.of(propietario));
        prepararDocumentoDisponible("999");

        strategy.asignarPerfil(usuario, request);

        PerfilProveedor perfil = capturarPerfilGuardado();
        assertSame(cedula, perfil.getTipoDocumento());
        assertEquals("999", perfil.getNumeroDocumento());
    }

    // H-02: Si solo llega tipo sin número, exigir número
    @Test
    void debeLanzarReglaNegocio_cuandoSoloLlegaTipoSinNumero() {
        request.setTipoDocumento("CE");
        request.setNumeroDocumento(null);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Número de documento es obligatorio", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
        verifyNoInteractions(perfilPropietarioRepository);
    }

    // H-02: Si solo llega número sin tipo, exigir tipo
    @Test
    void debeLanzarReglaNegocio_cuandoSoloLlegaNumeroSinTipo() {
        request.setTipoDocumento(null);
        request.setNumeroDocumento("999");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Tipo de documento es obligatorio", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
        verifyNoInteractions(perfilPropietarioRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoPropietarioNoTieneTipoDocumento() {
        request.setTipoDocumento(null);
        request.setNumeroDocumento(null);
        PerfilPropietario propietario = PerfilPropietario.builder().usuarioId(20).numeroDocumento("999").build();
        when(perfilPropietarioRepository.findById(20)).thenReturn(Optional.of(propietario));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Tipo de documento es obligatorio", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoPropietarioNoTieneNumeroDocumento() {
        request.setTipoDocumento(null);
        request.setNumeroDocumento(null);
        PerfilPropietario propietario = PerfilPropietario.builder().usuarioId(20).tipoDocumento(cedula).build();
        when(perfilPropietarioRepository.findById(20)).thenReturn(Optional.of(propietario));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Número de documento es obligatorio", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, NULL, Tipo de documento es obligatorio",
            "'  ', '', Tipo de documento es obligatorio"
    }, nullValues = "NULL")
    void debeLanzarReglaNegocio_cuandoFaltanDatosYNoHayPerfilPropietario(String tipoDocumento, String numeroDocumento, String mensaje) {
        request.setTipoDocumento(tipoDocumento);
        request.setNumeroDocumento(numeroDocumento);
        when(perfilPropietarioRepository.findById(20)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals(mensaje, ex.getMessage());
        verifyNoInteractions(tipoDocumentoRepository);
        verify(perfilProveedorRepository, never()).save(any());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExiste() {
        request.setTipoDocumento(" XX ");
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("XX")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Tipo de documento no válido: XX", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    @Test
    void debeLanzarRecursoDuplicado_cuandoDocumentoYaRegistrado() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "777888")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals("Ya existe un proveedor registrado con este documento.", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    static Stream<Arguments> datosProfesionalesInvalidos() {
        return Stream.of(
                caso("especialidad null", r -> r.setEspecialidad(null), MENSAJE_ESPECIALIDAD),
                caso("especialidad corta", r -> r.setEspecialidad("  M  "), MENSAJE_ESPECIALIDAD),
                caso("especialidad larga", r -> r.setEspecialidad("e".repeat(101)), MENSAJE_ESPECIALIDAD),
                caso("descripción larga", r -> r.setDescripcionProfesional("d".repeat(501)),
                        "La descripción profesional no puede superar los 500 caracteres.")
        );
    }

    @ParameterizedTest
    @MethodSource("datosProfesionalesInvalidos")
    void debeLanzarReglaNegocio_cuandoDatosProfesionalesSonInvalidos(Consumer<RolAsignacionRequest> modificacion, String mensaje) {
        modificacion.accept(request);
        prepararDocumentoDisponible("777888");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.asignarPerfil(usuario, request));

        assertEquals(mensaje, ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    @Test
    void debeEliminarPerfilProveedor_cuandoSeRemueveRol() {
        strategy.removerPerfil(20);

        verify(perfilProveedorRepository).deleteById(20);
    }

    private static Arguments caso(String nombre, Consumer<RolAsignacionRequest> modificacion, String mensaje) {
        return Arguments.of(Named.of(nombre, modificacion), mensaje);
    }
}
