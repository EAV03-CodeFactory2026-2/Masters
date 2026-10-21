package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilPropietario;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilPropietarioRepository;
import com.udea.masters.repositories.TipoDocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroPropietarioStrategyTest {

    private static final String MENSAJE_TELEFONO = "Ingresa un teléfono válido de 7 a 15 dígitos.";
    private static final String MENSAJE_NOMBRE = "Los nombres solo deben contener letras y tener entre 2 y 100 caracteres.";

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Mock
    private PerfilPropietarioRepository perfilPropietarioRepository;

    @InjectMocks
    private RegistroPropietarioStrategy strategy;

    private RegistroUsuarioRequest request;
    private TipoDocumento cedula;

    @BeforeEach
    void setUp() {
        cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
        request = RegistroUsuarioRequest.builder()
                .rol("Propietario")
                .nombre(" José Ángel ")
                .apellido("Muñoz")
                .telefono("3001234567")
                .tipoDocumento(" cc ")
                .numeroDocumento(" 98765432 ")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"propietario", "Propietario", "PROPIETARIO", " propietario "})
    void debeRetornarVerdadero_cuandoRolEsPropietario(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertTrue(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"cliente", "proveedor", "propietarios"})
    void debeRetornarFalso_cuandoRolNoEsPropietario(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertFalse(resultado);
    }

    @Test
    void debeRetornarPropietario_cuandoSeConsultaNombreRol() {
        String nombre = strategy.getNombreRol();

        assertEquals("Propietario", nombre);
    }

    static Stream<Arguments> datosObligatoriosInvalidos() {
        return Stream.of(
                caso("nombre y documento null", r -> {
                    r.setNombre(null);
                    r.setNumeroDocumento(null);
                }, "Nombres y Número de documento son obligatorios"),
                caso("nombre y documento vacíos", r -> {
                    r.setNombre(" ");
                    r.setNumeroDocumento("");
                }, "Nombres y Número de documento son obligatorios"),
                caso("nombre null", r -> r.setNombre(null), "Nombres es obligatorio"),
                caso("nombre vacío", r -> r.setNombre("   "), "Nombres es obligatorio"),
                caso("documento null", r -> r.setNumeroDocumento(null), "Número de documento es obligatorio"),
                caso("documento vacío", r -> r.setNumeroDocumento("  "), "Número de documento es obligatorio"),
                caso("nombre de una letra", r -> r.setNombre("J"), MENSAJE_NOMBRE),
                caso("nombre con números", r -> r.setNombre("Juan2"), MENSAJE_NOMBRE),
                caso("nombre con símbolos", r -> r.setNombre("Juan#"), MENSAJE_NOMBRE),
                caso("nombre largo", r -> r.setNombre("a".repeat(101)), MENSAJE_NOMBRE),
                caso("apellido null", r -> r.setApellido(null), "Apellidos es obligatorio"),
                caso("apellido vacío", r -> r.setApellido(" "), "Apellidos es obligatorio"),
                caso("tipo documento null", r -> r.setTipoDocumento(null), "Tipo de documento es obligatorio"),
                caso("tipo documento vacío", r -> r.setTipoDocumento(""), "Tipo de documento es obligatorio")
        );
    }

    @ParameterizedTest
    @MethodSource("datosObligatoriosInvalidos")
    void debeLanzarReglaNegocio_cuandoFaltanDatosObligatorios(Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        modificacion.accept(request);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals(mensaje, ex.getMessage());
        verifyNoInteractions(tipoDocumentoRepository, perfilPropietarioRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExiste() {
        request.setTipoDocumento("XX");
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("XX")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals("Tipo de documento no válido: XX", ex.getMessage());
        verifyNoInteractions(perfilPropietarioRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "1234567890123456", "300-123-4567"})
    void debeLanzarReglaNegocio_cuandoTelefonoEsInvalido(String telefono) {
        request.setTelefono(telefono);
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.of(cedula));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals(MENSAJE_TELEFONO, ex.getMessage());
        verify(perfilPropietarioRepository, never()).existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(any(), anyString());
    }

    @Test
    void debeLanzarRecursoDuplicado_cuandoDocumentoYaRegistrado() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.of(cedula));
        when(perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "98765432")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> strategy.validar(request));

        assertEquals("Ya existe un propietario registrado con este documento.", ex.getMessage());
    }

    @Test
    void debeValidarSinErrores_cuandoDatosSonValidos() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.of(cedula));
        when(perfilPropietarioRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "98765432")).thenReturn(false);

        assertDoesNotThrow(() -> strategy.validar(request));
    }

    @Test
    void debeGuardarPerfilPropietario_cuandoSeProcesaRegistro() {
        Usuario usuario = Usuario.builder().idUsuario(3).build();
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.of(cedula));

        strategy.procesarRegistro(request, usuario);

        ArgumentCaptor<PerfilPropietario> captor = ArgumentCaptor.forClass(PerfilPropietario.class);
        verify(perfilPropietarioRepository).save(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
        assertSame(cedula, captor.getValue().getTipoDocumento());
        assertEquals("98765432", captor.getValue().getNumeroDocumento());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExisteAlProcesarRegistro() {
        Usuario usuario = Usuario.builder().idUsuario(3).build();
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("cc")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.procesarRegistro(request, usuario));

        assertEquals("Tipo de documento no válido", ex.getMessage());
        verify(perfilPropietarioRepository, never()).save(any());
    }

    private static Arguments caso(String nombre, Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        return Arguments.of(Named.of(nombre, modificacion), mensaje);
    }
}
