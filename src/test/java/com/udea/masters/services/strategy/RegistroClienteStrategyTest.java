package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RegistroUsuarioRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.Usuario;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.PerfilClienteRepository;
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

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroClienteStrategyTest {

    private static final String MENSAJE_TELEFONO = "Ingresa un teléfono válido de 7 a 15 dígitos.";

    @Mock
    private PerfilClienteRepository perfilClienteRepository;

    @InjectMocks
    private RegistroClienteStrategy strategy;

    private RegistroUsuarioRequest request;

    @BeforeEach
    void setUp() {
        request = RegistroUsuarioRequest.builder()
                .rol("Cliente")
                .nombre("Laura")
                .apellido("Gómez")
                .telefono(" 3001234567 ")
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente", "Cliente", "CLIENTE", "  cliente  "})
    void debeRetornarVerdadero_cuandoRolEsCliente(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertTrue(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"proveedor", "propietario", "clientes"})
    void debeRetornarFalso_cuandoRolNoEsCliente(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertFalse(resultado);
    }

    @Test
    void debeRetornarCliente_cuandoSeConsultaNombreRol() {
        String nombre = strategy.getNombreRol();

        assertEquals("Cliente", nombre);
    }

    static Stream<Arguments> datosInvalidos() {
        return Stream.of(
                caso("nombre null", r -> r.setNombre(null), "Nombres es obligatorio"),
                caso("nombre vacío", r -> r.setNombre(" "), "Nombres es obligatorio"),
                caso("apellido null", r -> r.setApellido(null), "Apellidos es obligatorio"),
                caso("apellido vacío", r -> r.setApellido(""), "Apellidos es obligatorio"),
                caso("teléfono null", r -> r.setTelefono(null), "Teléfono de contacto es obligatorio"),
                caso("teléfono vacío", r -> r.setTelefono("  "), "Teléfono de contacto es obligatorio"),
                caso("teléfono corto", r -> r.setTelefono("123456"), MENSAJE_TELEFONO),
                caso("teléfono largo", r -> r.setTelefono("1234567890123456"), MENSAJE_TELEFONO),
                caso("teléfono con letras", r -> r.setTelefono("300abc4567"), MENSAJE_TELEFONO)
        );
    }

    @ParameterizedTest
    @MethodSource("datosInvalidos")
    void debeLanzarReglaNegocio_cuandoDatosSonInvalidos(Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        modificacion.accept(request);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> strategy.validar(request));

        assertEquals(mensaje, ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789012345"})
    void debeValidarSinErrores_cuandoTelefonoEstaEnLimites(String telefono) {
        request.setTelefono(telefono);

        assertDoesNotThrow(() -> strategy.validar(request));
    }

    @Test
    void debeGuardarPerfilCliente_cuandoSeProcesaRegistro() {
        Usuario usuario = Usuario.builder().idUsuario(4).build();

        strategy.procesarRegistro(request, usuario);

        ArgumentCaptor<PerfilCliente> captor = ArgumentCaptor.forClass(PerfilCliente.class);
        verify(perfilClienteRepository).save(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
    }

    private static Arguments caso(String nombre, Consumer<RegistroUsuarioRequest> modificacion, String mensaje) {
        return Arguments.of(Named.of(nombre, modificacion), mensaje);
    }
}
