package com.udea.masters.services.strategy;

import com.udea.masters.controllers.DTOs.RolAsignacionRequest;
import com.udea.masters.entities.PerfilCliente;
import com.udea.masters.entities.Usuario;
import com.udea.masters.repositories.PerfilClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolPerfilClienteStrategyTest {

    @Mock
    private PerfilClienteRepository perfilClienteRepository;

    @InjectMocks
    private RolPerfilClienteStrategy strategy;

    private Usuario usuario;
    private RolAsignacionRequest request;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(8).correo("ana@correo.com").build();
        request = RolAsignacionRequest.builder().nombreRol("Cliente").build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Cliente", "cliente", "CLIENTE"})
    void debeRetornarVerdadero_cuandoRolEsCliente(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertTrue(resultado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" Cliente ", "Proveedor", "Propietario"})
    void debeRetornarFalso_cuandoRolNoCoincideExactamente(String rol) {
        boolean resultado = strategy.soporta(rol);

        assertFalse(resultado);
    }

    @Test
    void debeRetornarCliente_cuandoSeConsultaNombreRol() {
        String nombre = strategy.getNombreRol();

        assertEquals("Cliente", nombre);
    }

    @Test
    void debeCrearPerfilCliente_cuandoNoExistePerfil() {
        when(perfilClienteRepository.existsById(8)).thenReturn(false);

        strategy.asignarPerfil(usuario, request);

        ArgumentCaptor<PerfilCliente> captor = ArgumentCaptor.forClass(PerfilCliente.class);
        verify(perfilClienteRepository).save(captor.capture());
        assertSame(usuario, captor.getValue().getUsuario());
    }

    @Test
    void debeOmitirCreacion_cuandoPerfilClienteYaExiste() {
        when(perfilClienteRepository.existsById(8)).thenReturn(true);

        strategy.asignarPerfil(usuario, request);

        verify(perfilClienteRepository, never()).save(any());
    }

    @Test
    void debeEliminarPerfilCliente_cuandoSeRemueveRol() {
        strategy.removerPerfil(8);

        verify(perfilClienteRepository).deleteById(8);
    }
}
