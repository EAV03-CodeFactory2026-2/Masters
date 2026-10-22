# Hallazgos de calidad — Sprint 2 — Masters

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | WebAPI Masters |
| Sprint | 2 |
| Fecha de verificación | 2026-10-22 |
| Rama y commit verificados | `main` @ `a3a6886` |
| Tests ejecutados | 509 |
| Fallos | 0 |
| Cobertura de líneas | 92,07 % (790/858) |
| Cobertura de ramas | 90,16 % (229/254) |
| Complejidad ciclomática del proyecto | 334 |
| Complejidad máxima por clase | 29 (`UsuarioService`) |

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Total |
|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 |
| Media | 2 | 0 | 0 | 2 |
| Baja | 6 | 0 | 0 | 6 |
| Por confirmar | 0 | 0 | 0 | 0 |
| Total | 8 | 0 | 0 | 8 |

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CP | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | H-01 | `NegocioService.crearNegocio` (L32) | La validación de identificación fiscal duplicada (L32) se ejecuta antes de verificar el rol Propietario (L36). Un usuario sin el rol recibe 409 en lugar de 400. | Un usuario no autorizado puede confirmar si un NIT está registrado (enumeración). Código HTTP inconsistente con la regla de negocio. | Media | Registro de negocios | Vigente | Validar el rol antes de consultar el repositorio. |
| H-02 | H-02 | `RolPerfilPropietarioStrategy.asignarPerfil` (L47-L50) / `RolPerfilProveedorStrategy.asignarPerfil` (L48-L51) | Si el request trae solo uno de los dos campos del documento, el faltante se toma del otro perfil del usuario. Puede combinar el tipo de documento del request con el número de otro perfil. El refactor reescribió el bloque con `Optional` sin cambiar la regla. | Perfil creado con un documento que no corresponde al usuario; afecta la integridad de datos y la validación de duplicados. | Media | Usuarios/Perfiles/Roles | Vigente | Reutilizar el documento del otro perfil solo si ambos campos vienen vacíos; si llega uno, exigir el otro. |
| H-03 | H-03 | `NegocioController.crearNegocio` (L40), `NegocioController.listarMisNegocios` (L64), `ServicioController.crearServicio` (L41), `UsuarioController.obtenerMiPerfil` (L42) | `Integer.parseInt(principal.getName())` sin validación. Un `name` no numérico produce `NumberFormatException`, que se captura como `IllegalArgumentException` y responde 400 con el mensaje crudo ("For input string…"). Con `principal` nulo se produce NPE (500). | Exposición de mensajes internos y códigos HTTP incorrectos ante tokens mal formados. Riesgo bajo mientras el filtro JWT garantice un id numérico. | Baja | Transversal (seguridad) | Vigente | Obtener el id desde `IAuthenticationFacade` y responder 401 cuando no exista un usuario autenticado válido. |
| H-04 | H-04 | `RolPerfilClienteStrategy.soporta` (L19), `RolPerfilPropietarioStrategy.soporta` (L30), `RolPerfilProveedorStrategy.soporta` (L31) | No aplican `trim()` al nombre del rol, a diferencia de `Registro*Strategy.soporta`. `" Proveedor "` no es soportado. | Comportamiento inconsistente entre estrategias; no se manifiesta hoy porque `UsuarioService` envía el nombre tal como está en BD. | Baja | Usuarios/Perfiles/Roles | Vigente | Unificar la normalización (`trim` + `equalsIgnoreCase`) en ambas familias de estrategias. |
| H-05 | H-05 | `RegistroService.registrar` (L51) | Un rol sin estrategia (p. ej. "Administrador") devuelve el mismo mensaje que un rol vacío (L45): "Debes indicar el rol con el que te registras." | El usuario no distingue entre rol omitido y rol inválido. | Baja | Usuarios/Perfiles/Roles | Vigente | Mensaje específico para rol no soportado (p. ej. "El rol indicado no es válido"). |
| H-06 | H-06 | `RecursoValidator.validar` (L52) | El refactor integró `validarCantidad` en `validar` y cambió la expresión a `\+?0*[1-9]\d{0,2}`. Sigue aceptando el signo `+`: `"+8"` se interpreta como 8. | Incumple el criterio "solo dígitos" de la HU. No hay corrupción de datos: el valor queda en el rango 1..999. | Baja | HU-0010 | Vigente | Eliminar `\+?` de la expresión para entradas de tipo texto. |
| H-07 | H-07 | `UsuarioService.listarPorRol` (L70-L72) | Para el rol Propietario sin usuario autenticado retorna lista vacía, mientras `listarUsuarios` (L58) retorna todos los registros en la misma condición (`esUsuarioVisibleParaConsulta`, L166). | Inconsistencia en las reglas de visibilidad; no alcanzable hoy porque el endpoint exige rol Propietario. | Baja | Usuarios/Perfiles/Roles | Vigente | Unificar la política de visibilidad sin autenticación en ambos métodos. |
| H-08 | H-08 | `HorarioValidator.parsearDia` (L134) | Con día nulo el mensaje se construye como "El día 'null' no es válido…". | Mensaje poco claro para el cliente de la API. | Baja | Horario Proveedores | Vigente | Mensaje específico para día ausente (p. ej. "El día es obligatorio."). |

## 4. Pendientes de definir con el PO

No aplica.

## 5. Complejidad ciclomática

| Métrica | Antes (`3ca3bee`) | Después (`a3a6886`) |
|---|---|---|
| Complejidad del proyecto | 447 | 334 |
| Complejidad máxima por clase | 42 (`UsuarioService`) | 29 (`UsuarioService`) |

| Clase | Complejidad |
|---|---|
| `UsuarioService` | 29 |
| `HorarioValidator` | 27 |
| `RegistroProveedorStrategy` | 21 |
| `RegistroPropietarioStrategy` | 17 |
| `RolPerfilProveedorStrategy` | 15 |

El pipeline evalúa `complexity < 50`. Falta confirmar con el profesor si el límite aplica al proyecto o a la clase.

| Criterio | Antes | Después |
|---|---|---|
| Por proyecto (< 50) | No cumple (447) | No cumple (334) |
| Por clase (< 50) | Cumple (42) | Cumple (29) |

## 6. Notas

- Los hallazgos no se corrigen desde pruebas unitarias porque el alcance excluye el código de producción. Los de severidad Alta y Media se registran en Mantis.
- Tests ajustados por el refactor: ninguno. La suite compiló y pasó sin cambios (509 tests, 0 fallos).
- La columna "Antes" se obtuvo compilando y ejecutando `mvn clean verify` sobre el commit `3ca3bee` (merge de `feature/unit-tests-sprint2`): 509 tests, 91,72 % de líneas (798/870) y 90,77 % de ramas (403/444).
- El total de ramas medido por JaCoCo bajó de 444 a 254 porque varias validaciones de nulos y vacíos pasaron a `Optional`, `StringUtils` y `Objects`, cuya lógica condicional no se instrumenta. Parte de la reducción de complejidad corresponde a ese traslado y no a menos reglas de negocio.
- El refactor agregó `sonar.exclusions` en `pom.xml` (`config`, `entities`, `security`, `services`, `DTOs`, `*Application`). SonarCloud deja de analizar la capa de servicios y seguridad (cobertura, complejidad e issues); las cifras de este documento provienen de JaCoCo local e incluyen esas capas. Validar con el profesor que la exclusión sea aceptable para el Quality Gate.
