package com.udea.masters.services;

import com.udea.masters.entities.Moneda;
import com.udea.masters.repositories.MonedaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonedaServiceTest {

    @Mock
    private MonedaRepository monedaRepository;

    @InjectMocks
    private MonedaService monedaService;

    @Test
    void debeRetornarMonedas_cuandoExistenEnCatalogo() {
        List<Moneda> monedas = List.of(
                Moneda.builder().monedaCodigoIso("COP").nombreMoneda("Peso Colombiano").build(),
                Moneda.builder().monedaCodigoIso("USD").nombreMoneda("Dólar Estadounidense").build());
        when(monedaRepository.findAll()).thenReturn(monedas);

        List<Moneda> resultado = monedaService.listarMonedas();

        assertEquals(monedas, resultado);
        verify(monedaRepository).findAll();
    }

    @Test
    void debeRetornarListaVacia_cuandoCatalogoSinMonedas() {
        when(monedaRepository.findAll()).thenReturn(List.of());

        List<Moneda> resultado = monedaService.listarMonedas();

        assertTrue(resultado.isEmpty());
    }
}
