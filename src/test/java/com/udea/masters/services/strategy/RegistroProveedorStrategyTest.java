package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilProveedor;
import com.udea.masters.entities.TipoDocumento;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilProveedorRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroProveedorStrategyTest {

    private static final String MENSAJE_ESPECIALIDAD = "La especialidad debe tener entre 2 y 100 caracteres.";

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @InjectMocks
    private RegistroProveedorStrategy strategy;

    private RegistroUsuarioRequest request;
    private TipoDocumento cedula;

    @BeforeEach
    void setUp() {
        cedula = TipoDocumento.builder().idTipoDocumento(1).codigo("CC").nombre("Cédula de ciudadanía").build();
        request = RegistroUsuarioRequest.builder()
                .rol("Proveedor")
                .nombre("Carlos")
                .apellido("Ruiz")
                .telefono(" 3001234567 ")
                .tipoDocumento(" CC ")
                .numeroDocumento(" 1020304050 ")
                .especialidad("  Barbería  ")
                .descripcionProfesional("  Cortes clásicos  ")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"proveedor", "Proveedor", "PROVEEDOR", "  proveedor  "})
    void debeRetornarVerdadero_cuandoRolEsProveedor(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertTrue(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"cliente", "propietario", "proveedores"})
    void debeRetornarFalso_cuandoRolNoEsProveedor(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertFalse(resultado);
    }

    @Test
    void debeRetornarProveedor_cuandoSeConsultaNombreRol() {
        String nombre = strategy.getNombreRol();

        assertEquals("Proveedor", nombre);
    }

    static Stream<Arguments> datosObligatoriosInvalidos() {
        return Stream.of(
                caso("nombre null", r -> r.setNombre(null), "Nombres es obligatorio"),
                caso("nombre vacío", r -> r.setNombre("  "), "Nombres es obligatorio"),
                caso("apellido null", r -> r.setApellido(null), "Apellidos es obligatorio"),
                caso("apellido vacío", r -> r.setApellido(""), "Apellidos es obligatorio"),
                caso("teléfono null", r -> r.setTelefono(null), "Ingresa un teléfono válido de 7 a 15 dígitos."),
                caso("teléfono corto", r -> r.setTelefono("123456"), "Ingresa un teléfono válido de 7 a 15 dígitos."),
                caso("teléfono con letras", r -> r.setTelefono("300ABC4567"), "Ingresa un teléfono válido de 7 a 15 dígitos."),
                caso("teléfono largo", r -> r.setTelefono("1234567890123456"), "Ingresa un teléfono válido de 7 a 15 dígitos."),
                caso("faltan documento y especialidad", r -> {
                    r.setTipoDocumento(null);
                    r.setNumeroDocumento(" ");
                    r.setEspecialidad(null);
                }, "Los campos Tipo de documento, Número de documento y Especialidad son obligatorios para el rol Proveedor."),
                caso("faltan tipo y número de documento", r -> {
                    r.setTipoDocumento("");
                    r.setNumeroDocumento(null);
                }, "Tipo de documento es obligatorio"),
                caso("tipo documento null", r -> r.setTipoDocumento(null), "Tipo de documento es obligatorio"),
                caso("tipo documento vacío", r -> r.setTipoDocumento(" "), "Tipo de documento es obligatorio"),
                caso("número documento null", r -> r.setNumeroDocumento(null), "Número de documento es obligatorio"),
                caso("número documento vacío", r -> r.setNumeroDocumento(""), "Número de documento es obligatorio")
        );
    }

    @ParameterizedTest
    @MethodSource("datosObligatoriosInvalidos")
    void debeLanzarReglaNegocio_cuandoFaltanDatosObligatorios(Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        modificacion.accept(request);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals(mensaje, ex.getMessage());
        verifyNoInteractions(tipoDocumentoRepository, perfilProveedorRepository);
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExiste() {
        request.setTipoDocumento("XX");
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("XX")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals("Tipo de documento no válido: XX", ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
    }

    @Test
    void debeLanzarRecursoDuplicado_cuandoDocumentoYaRegistrado() {
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "1020304050")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class, () -> strategy.validar(request));

        assertEquals("Ya existe un proveedor registrado con este documento.", ex.getMessage());
    }

    static Stream<Arguments> datosProfesionalesInvalidos() {
        return Stream.of(
                caso("especialidad null", r -> r.setEspecialidad(null), MENSAJE_ESPECIALIDAD),
                caso("especialidad vacía", r -> r.setEspecialidad("   "), MENSAJE_ESPECIALIDAD),
                caso("especialidad corta", r -> r.setEspecialidad(" B "), MENSAJE_ESPECIALIDAD),
                caso("especialidad larga", r -> r.setEspecialidad("a".repeat(101)), MENSAJE_ESPECIALIDAD),
                caso("descripción larga", r -> r.setDescripcionProfesional("d".repeat(501)),
                        "La descripción profesional no puede superar los 500 caracteres.")
        );
    }

    @ParameterizedTest
    @MethodSource("datosProfesionalesInvalidos")
    void debeLanzarReglaNegocio_cuandoDatosProfesionalesSonInvalidos(Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        modificacion.accept(request);
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "1020304050")).thenReturn(false);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals(mensaje, ex.getMessage());
    }

    @Test
    void debeValidarSinErrores_cuandoDatosSonValidosYDescripcionEsNull() {
        request.setDescripcionProfesional(null);
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "1020304050")).thenReturn(false);

        assertDoesNotThrow(() -> strategy.validar(request));
    }

    @Test
    void debeAceptarLimites_cuandoEspecialidadYDescripcionTienenLongitudMaxima() {
        request.setEspecialidad("e".repeat(100));
        request.setDescripcionProfesional("d".repeat(500));
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));
        when(perfilProveedorRepository.existsByTipoDocumentoAndNumeroDocumentoIgnoreCase(cedula, "1020304050")).thenReturn(false);

        assertDoesNotThrow(() -> strategy.validar(request));
    }

    @Test
    void debeGuardarPerfilProveedor_cuandoSeProcesaRegistro() {
        Usuario usuario = Usuario.builder().idUsuario(5).correo("carlos@correo.com").build();
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));

        strategy.procesarRegistro(request, usuario);

        ArgumentCaptor<PerfilProveedor> captor = ArgumentCaptor.forClass(PerfilProveedor.class);
        verify(perfilProveedorRepository).save(captor.capture());
        PerfilProveedor perfil = captor.getValue();
        assertSame(usuario, perfil.getUsuario());
        assertSame(cedula, perfil.getTipoDocumento());
        assertEquals("1020304050", perfil.getNumeroDocumento());
        assertEquals("Barbería", perfil.getEspecialidad());
        assertEquals("Cortes clásicos", perfil.getDescripcionProfesional());
    }

    @Test
    void debeGuardarDescripcionNull_cuandoNoSeEnviaDescripcion() {
        request.setDescripcionProfesional(null);
        Usuario usuario = Usuario.builder().idUsuario(5).build();
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.of(cedula));

        strategy.procesarRegistro(request, usuario);

        ArgumentCaptor<PerfilProveedor> captor = ArgumentCaptor.forClass(PerfilProveedor.class);
        verify(perfilProveedorRepository).save(captor.capture());
        assertNull(captor.getValue().getDescripcionProfesional());
    }

    @Test
    void debeLanzarReglaNegocio_cuandoTipoDocumentoNoExisteAlProcesarRegistro() {
        Usuario usuario = Usuario.builder().idUsuario(5).build();
        when(tipoDocumentoRepository.findByCodigoIgnoreCase("CC")).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.procesarRegistro(request, usuario));

        assertEquals("Tipo de documento no válido", ex.getMessage());
        verify(perfilProveedorRepository, never()).save(any());
    }

    private static Arguments caso(String nombre, Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        return Arguments.of(Named.of(nombre, modificacion), mensaje);
    }
}
