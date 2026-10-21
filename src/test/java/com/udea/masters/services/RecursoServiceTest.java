package com.udea.masters.services;

import com.udea.masters.controllers.DTOs.RecursoCreateRequest;
import com.udea.masters.controllers.DTOs.RecursoRegistradoResponse;
import com.udea.masters.controllers.DTOs.RecursoResponse;
import com.udea.masters.controllers.DTOs.RecursosNegocioResponse;
import com.udea.masters.entities.Negocio;
import com.udea.masters.entities.Recurso;
import com.udea.masters.entities.TipoRecurso;
import com.udea.masters.entities.TipoRol;
import com.udea.masters.exceptions.AccesoDenegadoException;
import com.udea.masters.exceptions.RecursoDuplicadoException;
import com.udea.masters.exceptions.RecursoNoEncontradoException;
import com.udea.masters.exceptions.ReglaNegocioException;
import com.udea.masters.repositories.NegocioRepository;
import com.udea.masters.repositories.RecursoRepository;
import com.udea.masters.security.IAuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {

    private static final String PROPIETARIO = TipoRol.PROPIETARIO.getNombre();

    @Mock
    private RecursoRepository recursoRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private RecursoValidator recursoValidator;

    @Mock
    private IAuthenticationFacade authenticationFacade;

    @InjectMocks
    private RecursoService recursoService;

    private Negocio negocio;
    private RecursoCreateRequest request;
    private RecursoValidator.DatosRecurso datos;

    @BeforeEach
    void setUp() {
        negocio = Negocio.builder()
                .idNegocio(5)
                .propietarioId(10)
                .nombreNegocio("Spa Relax")
                .build();

        request = RecursoCreateRequest.builder()
                .nombre("Camilla de masajes")
                .tipo("Equipo")
                .cantidad(3)
                .descripcion("Plegable")
                .build();

        datos = new RecursoValidator.DatosRecurso(
                "Camilla de masajes", "camilla de masajes", TipoRecurso.EQUIPO, 3, "Plegable");
    }

    private void accesoPermitido() {
        when(authenticationFacade.tieneRol(PROPIETARIO)).thenReturn(true);
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(10);
    }

    private Recurso recurso(Integer id, String nombre, TipoRecurso tipo) {
        return Recurso.builder()
                .idRecurso(id)
                .negocio(negocio)
                .nombre(nombre)
                .nombreNormalizado(nombre.toLowerCase())
                .tipo(tipo)
                .cantidad(2)
                .build();
    }

    @Test
    void debeRegistrarRecursoActivo_cuandoPropietarioYDatosValidos() {
        accesoPermitido();
        when(recursoValidator.validar(request)).thenReturn(datos);
        when(recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(5, "camilla de masajes")).thenReturn(false);
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> {
            Recurso r = inv.getArgument(0);
            r.setIdRecurso(77);
            return r;
        });
        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);

        RecursoRegistradoResponse response = recursoService.registrarRecurso(5, request);

        verify(recursoRepository).saveAndFlush(captor.capture());
        Recurso guardado = captor.getValue();
        assertSame(negocio, guardado.getNegocio());
        assertEquals("Camilla de masajes", guardado.getNombre());
        assertEquals("camilla de masajes", guardado.getNombreNormalizado());
        assertEquals(TipoRecurso.EQUIPO, guardado.getTipo());
        assertEquals(3, guardado.getCantidad());
        assertEquals("Plegable", guardado.getDescripcion());
        assertEquals(Recurso.ESTADO_ACTIVO, guardado.getEstado());

        assertEquals(RecursoService.MSG_REGISTRADO, response.getMensaje());
        RecursoResponse recurso = response.getRecurso();
        assertEquals(77, recurso.getIdRecurso());
        assertEquals(5, recurso.getNegocioId());
        assertEquals("Camilla de masajes", recurso.getNombre());
        assertEquals("Equipo", recurso.getTipo());
        assertEquals(3, recurso.getCantidad());
        assertEquals("Plegable", recurso.getDescripcion());
        assertEquals("Activo", recurso.getEstado());
    }

    static Stream<Arguments> operaciones() {
        RecursoCreateRequest req = RecursoCreateRequest.builder().nombre("Sala").build();
        return Stream.of(
                Arguments.of("registrar", (Function<RecursoService, Executable>) s -> () -> s.registrarRecurso(5, req)),
                Arguments.of("listar", (Function<RecursoService, Executable>) s -> () -> s.listarRecursos(5))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    void debeLanzar403_cuandoRolActivoNoEsPropietario(String operacion, Function<RecursoService, Executable> llamada) {
        when(authenticationFacade.tieneRol(PROPIETARIO)).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, llamada.apply(recursoService));

        assertEquals(RecursoService.MSG_SOLO_PROPIETARIO, ex.getMessage());
        verifyNoInteractions(negocioRepository, recursoRepository, recursoValidator);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    void debeLanzar404_cuandoNegocioNoExiste(String operacion, Function<RecursoService, Executable> llamada) {
        when(authenticationFacade.tieneRol(PROPIETARIO)).thenReturn(true);
        when(negocioRepository.findById(5)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                llamada.apply(recursoService));

        assertEquals(RecursoService.MSG_NEGOCIO_NO_ENCONTRADO, ex.getMessage());
        verifyNoInteractions(recursoRepository, recursoValidator);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    void debeLanzar403_cuandoNegocioPerteneceAOtroPropietario(String operacion,
                                                              Function<RecursoService, Executable> llamada) {
        when(authenticationFacade.tieneRol(PROPIETARIO)).thenReturn(true);
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(99);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, llamada.apply(recursoService));

        assertEquals(RecursoService.MSG_SIN_PERMISO_NEGOCIO, ex.getMessage());
        verifyNoInteractions(recursoRepository, recursoValidator);
    }

    @Test
    void debeLanzar403_cuandoUsuarioAutenticadoEsNulo() {
        when(authenticationFacade.tieneRol(PROPIETARIO)).thenReturn(true);
        when(negocioRepository.findById(5)).thenReturn(Optional.of(negocio));
        when(authenticationFacade.getAuthenticatedUserId()).thenReturn(null);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> recursoService.registrarRecurso(5, request));

        assertEquals(RecursoService.MSG_SIN_PERMISO_NEGOCIO, ex.getMessage());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @Test
    void debeLanzar400_cuandoValidadorRechazaDatos() {
        accesoPermitido();
        when(recursoValidator.validar(request))
                .thenThrow(new ReglaNegocioException(RecursoValidator.MSG_CANTIDAD_INVALIDA));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> recursoService.registrarRecurso(5, request));

        assertEquals(RecursoValidator.MSG_CANTIDAD_INVALIDA, ex.getMessage());
        verify(recursoRepository, never()).existsByNegocioIdNegocioAndNombreNormalizado(anyInt(), anyString());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @Test
    void debeLanzar409_cuandoNombreYaExisteEnElNegocio() {
        accesoPermitido();
        when(recursoValidator.validar(request)).thenReturn(datos);
        when(recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(5, "camilla de masajes")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> recursoService.registrarRecurso(5, request));

        assertEquals(RecursoService.MSG_NOMBRE_DUPLICADO, ex.getMessage());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @Test
    void debeLanzar409_cuandoRestriccionUnicaFallaAlGuardar() {
        accesoPermitido();
        when(recursoValidator.validar(request)).thenReturn(datos);
        when(recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(5, "camilla de masajes")).thenReturn(false);
        when(recursoRepository.saveAndFlush(any(Recurso.class)))
                .thenThrow(new DataIntegrityViolationException("uk_recurso_negocio_nombre_normalizado"));

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> recursoService.registrarRecurso(5, request));

        assertEquals(RecursoService.MSG_NOMBRE_DUPLICADO, ex.getMessage());
    }

    @Test
    void debeRetornarListaVaciaConMensaje_cuandoNegocioSinRecursos() {
        accesoPermitido();
        when(recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(5)).thenReturn(List.of());

        RecursosNegocioResponse response = recursoService.listarRecursos(5);

        assertEquals(RecursoService.MSG_LISTA_VACIA, response.getMensaje());
        assertTrue(response.getRecursos().isEmpty());
    }

    @Test
    void debeRetornarRecursosMapeados_cuandoNegocioTieneRecursos() {
        accesoPermitido();
        when(recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(5)).thenReturn(List.of(
                recurso(1, "Cabina", TipoRecurso.ESPACIO),
                recurso(2, "Camilla", TipoRecurso.EQUIPO)));

        RecursosNegocioResponse response = recursoService.listarRecursos(5);

        assertEquals(RecursoService.MSG_LISTA, response.getMensaje());
        assertEquals(2, response.getRecursos().size());
        RecursoResponse primero = response.getRecursos().get(0);
        assertEquals(1, primero.getIdRecurso());
        assertEquals(5, primero.getNegocioId());
        assertEquals("Cabina", primero.getNombre());
        assertEquals("Espacio", primero.getTipo());
        assertEquals(2, primero.getCantidad());
        assertNull(primero.getDescripcion());
        assertEquals(Recurso.ESTADO_ACTIVO, primero.getEstado());
        assertEquals("Equipo", response.getRecursos().get(1).getTipo());
    }
}
