# Hallazgos de calidad — Sprint 2 — Masters

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | WebAPI Masters |
| Sprint | 2 |
| Tipo de verificación | Verificación final del Sprint 2 |
| Fecha de verificación | 2026-10-27 |
| Rama y commit verificados | `main` @ `51cfe0e` |
| Línea base | `25c726b` (merge del PR #14) |
| Tests ejecutados | 518 |
| Fallos | 0 |
| Cobertura de líneas (JaCoCo) | 92,44 % (832/900) |
| Cobertura de ramas (JaCoCo) | 91,07 % (255/280) |
| Complejidad ciclomática del proyecto (JaCoCo) | 352 |
| Complejidad máxima por clase | 31 (`UsuarioService`) |
| SonarCloud (`main` @ `51cfe0e`) | Quality Gate OK · cobertura 100,0 % · complejidad 49 · 0 bugs · 0 vulnerabilidades |

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Cerrados por decisión | Total |
|---|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 | 0 |
| Media | 2 | 2 | 0 | 0 | 4 |
| Baja | 0 | 6 | 0 | 0 | 6 |
| Por confirmar | 0 | 0 | 0 | 0 | 0 |
| Total | 2 | 8 | 0 | 0 | 10 |

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CP | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | H-01 | `NegocioService.crearNegocio` (L32) | La validación de identificación fiscal duplicada (L32) se ejecuta antes de verificar el rol Propietario (L36). Un usuario sin el rol recibe 409 en lugar de 400. | Un usuario no autorizado puede confirmar si un NIT está registrado (enumeración). Código HTTP inconsistente con la regla de negocio. | Media | Registro de negocios | Corregido en `9a16c03` (PR #15). El rol se valida en L33 y la consulta del NIT pasa a L37. | Validar el rol antes de consultar el repositorio. |
| H-02 | H-02 | `RolPerfilPropietarioStrategy.asignarPerfil` (L47-L50) / `RolPerfilProveedorStrategy.asignarPerfil` (L48-L51) | Si el request trae solo uno de los dos campos del documento, el faltante se toma del otro perfil del usuario. Puede combinar el tipo de documento del request con el número de otro perfil. El refactor reescribió el bloque con `Optional` sin cambiar la regla. | Perfil creado con un documento que no corresponde al usuario; afecta la integridad de datos y la validación de duplicados. | Media | Usuarios/Perfiles/Roles | Corregido en `9a16c03` (PR #15). Solo se reutiliza el documento si faltan ambos campos; si llega uno, se exige el otro (`RolPerfilPropietarioStrategy` L50-L61, `RolPerfilProveedorStrategy` L51-L62). | Reutilizar el documento del otro perfil solo si ambos campos vienen vacíos; si llega uno, exigir el otro. |
| H-03 | H-03 | `NegocioController.crearNegocio` (L40), `NegocioController.listarMisNegocios` (L64), `ServicioController.crearServicio` (L41), `UsuarioController.obtenerMiPerfil` (L42) | `Integer.parseInt(principal.getName())` sin validación. Un `name` no numérico produce `NumberFormatException`, que se captura como `IllegalArgumentException` y responde 400 con el mensaje crudo ("For input string…"). Con `principal` nulo se produce NPE (500). | Exposición de mensajes internos y códigos HTTP incorrectos ante tokens mal formados. Riesgo bajo mientras el filtro JWT garantice un id numérico. | Baja | Transversal (seguridad) | Corregido en `9a16c03` (PR #15). Los cuatro métodos obtienen el id desde `IAuthenticationFacade` y responden 401 si es nulo (`NegocioController` L43 y L73, `ServicioController` L44, `UsuarioController` L45). `AuthenticationFacade` ya retornaba `null` ante un id no numérico. | Obtener el id desde `IAuthenticationFacade` y responder 401 cuando no exista un usuario autenticado válido. |
| H-04 | H-04 | `RolPerfilClienteStrategy.soporta` (L19), `RolPerfilPropietarioStrategy.soporta` (L30), `RolPerfilProveedorStrategy.soporta` (L31) | No aplican `trim()` al nombre del rol, a diferencia de `Registro*Strategy.soporta`. `" Proveedor "` no es soportado. | Comportamiento inconsistente entre estrategias; no se manifiesta hoy porque `UsuarioService` envía el nombre tal como está en BD. | Baja | Usuarios/Perfiles/Roles | Corregido en `9a16c03` (PR #15). Las tres estrategias aplican `trim()` + `equalsIgnoreCase` (`RolPerfilClienteStrategy` L22, `RolPerfilPropietarioStrategy` L31, `RolPerfilProveedorStrategy` L32). | Unificar la normalización (`trim` + `equalsIgnoreCase`) en ambas familias de estrategias. |
| H-05 | H-05 | `RegistroService.registrar` (L51) | Un rol sin estrategia (p. ej. "Administrador") devuelve el mismo mensaje que un rol vacío (L45): "Debes indicar el rol con el que te registras." | El usuario no distingue entre rol omitido y rol inválido. | Baja | Usuarios/Perfiles/Roles | Corregido en `9a16c03` (PR #15). L52 responde "El rol indicado no es válido." | Mensaje específico para rol no soportado (p. ej. "El rol indicado no es válido"). |
| H-06 | H-06 | `RecursoValidator.validar` (L52) | El refactor integró `validarCantidad` en `validar` y cambió la expresión a `\+?0*[1-9]\d{0,2}`. Sigue aceptando el signo `+`: `"+8"` se interpreta como 8. | Incumple el criterio "solo dígitos" de la HU. No hay corrupción de datos: el valor queda en el rango 1..999. | Baja | HU-0010 | Corregido en `9a16c03` (PR #15). La expresión pasa a la constante `PATRON_CANTIDAD` (L32, `0*[1-9]\d{0,2}`) y se aplica en L55; `"+8"` se rechaza. | Eliminar `\+?` de la expresión para entradas de tipo texto. |
| H-07 | H-07 | `UsuarioService.listarPorRol` (L70-L72) | Para el rol Propietario sin usuario autenticado retorna lista vacía, mientras `listarUsuarios` (L58) retorna todos los registros en la misma condición (`esUsuarioVisibleParaConsulta`, L166). | Inconsistencia en las reglas de visibilidad; no alcanzable hoy porque el endpoint exige rol Propietario. | Baja | Usuarios/Perfiles/Roles | Corregido en `9a16c03` (PR #15). L73 permite la visibilidad sin usuario autenticado, igual que `esUsuarioVisibleParaConsulta` (L168). | Unificar la política de visibilidad sin autenticación en ambos métodos. |
| H-08 | H-08 | `HorarioValidator.parsearDia` (L134) | Con día nulo el mensaje se construye como "El día 'null' no es válido…". | Mensaje poco claro para el cliente de la API. | Baja | Horario Proveedores | Corregido en `9a16c03` (PR #15). L132-L133 lanzan `MSG_DIA_OBLIGATORIO` ("El día es obligatorio.", L36). | Mensaje específico para día ausente (p. ej. "El día es obligatorio."). |
| H-09 | H-09 | `NegocioController.listarNegociosDisponibles` (L93-L96) / `NegocioService.listarNegociosDisponibles` (L72) | El endpoint `GET /api/v1/negocios/disponibles` devuelve la entidad JPA `Negocio` completa a cualquier usuario autenticado, incluidos `identificacionFiscal` y `propietarioId`. | Cualquier cuenta Cliente (el registro es abierto) obtiene la identificación fiscal de todos los negocios activos y el id interno de su propietario, lo que reabre la enumeración de NIT que cerró H-01. El contrato de la API queda acoplado al modelo de persistencia. | Media | HU-03 | Vigente | Responder con un DTO de catálogo que omita `identificacionFiscal` y `propietarioId`. |
| H-10 | H-10 | `UsuarioDataSeeder.run` (L29) / `application.properties` (L19) | `app.admin.password` tiene `admin123` como valor por defecto, repetido en `.env.example`. Si el despliegue no define `ADMIN_PASSWORD`, el seeder crea al arrancar una cuenta con rol Propietario y contraseña conocida. | Acceso con credenciales públicas a los endpoints restringidos a Propietario (listado de usuarios con datos personales, registro de negocios y servicios). Depende de la configuración del despliegue en GCE, que no se pudo verificar desde el repositorio. | Media | Transversal (seguridad) | Vigente | Eliminar el valor por defecto y abortar el arranque si `ADMIN_PASSWORD` no está definida. Si el administrador ya se creó con `admin123`, cambiar la contraseña. |

## 4. Pendientes de definir con el PO

No aplica.

## 5. Complejidad ciclomática

| Métrica | Antes (`25c726b`) | Después (`51cfe0e`) |
|---|---|---|
| Complejidad del proyecto (JaCoCo, todo el código) | 334 | 352 |
| Complejidad con `sonar.exclusions` (JaCoCo) | 45 | 50 |
| Complejidad del proyecto (SonarCloud) | No disponible | 49 |
| Complejidad máxima por clase | 29 (`UsuarioService`) | 31 (`UsuarioService`) |

| Clase | Antes | Después |
|---|---|---|
| `UsuarioService` | 29 | 31 |
| `HorarioValidator` | 27 | 28 |
| `RegistroProveedorStrategy` | 21 | 21 |
| `RolPerfilProveedorStrategy` | 15 | 17 |
| `RegistroPropietarioStrategy` | 17 | 17 |

El pipeline evalúa `complexity < 50` sobre la métrica de proyecto que reporta SonarCloud, calculada solo sobre el código no excluido por `sonar.exclusions`.

| Criterio | Antes | Después |
|---|---|---|
| Por proyecto en SonarCloud (< 50) | No disponible | Cumple (49) |
| Por proyecto en JaCoCo, todo el código (< 50) | No cumple (334) | No cumple (352) |
| Por clase (< 50) | Cumple (29) | Cumple (31) |

## 6. Notas

- Los hallazgos H-01 a H-08 los corrigió el equipo en `9a16c03`, integrado en `main` con el PR #15 (`4564c4c`). Esta verificación no modificó código de producción. H-09 y H-10 se registran en Mantis por ser de severidad Media.
- Tests agregados o ajustados en esta verificación: ninguno. La suite pasó sin cambios (518 tests, 0 fallos) y la cobertura de líneas (92,44 %) supera el 85 %.
- Tests modificados por el equipo desde `25c726b`: no se eliminaron métodos de prueba ni se agregó `@Disabled`; la suite pasó de 509 a 518 tests. Los cambios de `NegocioServiceTest`, `UsuarioServiceTest`, `RegistroServiceTest`, `HorarioValidatorTest`, los tests de controllers y los de `RolPerfil*StrategyTest` corresponden a los nuevos comportamientos de H-01 a H-08.
- `RecursoValidatorTest` (`9a16c03`) perdió casos de borde sin relación con H-06: la resolución del tipo pasó de `assertEquals(esperado, …)` a `assertNotNull(…)` y ya no prueba tipos con tilde (`ESPÁCIO`, `équipo`); se retiraron los casos de nombre que supera 100 caracteres tras colapsar espacios, de nombre y descripción en el límite después de recortar, de tipos inválidos parecidos (`Espacios`, `Equipo Espacio`) y del orden de validación con todos los campos inválidos. La cobertura de la clase sigue en 100 %, pero esas reglas de HU-0010 quedan sin aserción.
- En `RolPerfilPropietarioStrategyTest` y `RolPerfilProveedorStrategyTest` los casos con un solo campo de documento se prueban solo con `null`, no con texto en blanco.
- Código nuevo sin pruebas: todas las clases modificadas desde `25c726b` tienen 100 % de líneas y ramas. `findByEstado("Activo")` solo se prueba con mock; no hay prueba que confirme contra BD que los negocios inactivos se excluyen, ni de que `/api/v1/negocios/disponibles` exija autenticación en la cadena de seguridad.
- Observaciones menores:
  - Los controllers y servicios de `src/main` y varios tests incluyen comentarios `// H-0x: …` que describen el hallazgo y no el código.
  - `NegocioController.listarNegociosDisponibles` (L94) usa `java.util.List` calificado en lugar de importarlo.
  - CORS usa `allowCredentials(true)` con `https://*.vercel.app` y `http://localhost:*` como valor por defecto (`SecurityConfig` L35, `application.properties` L22). Cualquier aplicación publicada en `vercel.app` queda como origen permitido. La API autentica con token Bearer y no con cookies, por lo que el riesgo es bajo; conviene definir `CORS_ALLOWED_ORIGINS` en producción con el origen exacto del frontend.
  - `README.md` sigue describiendo la base de datos como Supabase (L28 y L329). `.env.example` no menciona Supabase.
- Migración de la base de datos de Supabase a PostgreSQL en Google Cloud Compute Engine: los tests usan H2 en memoria (`src/test/resources/application.properties`, `MODE=PostgreSQL`, `create-drop`) y no dependen de la base real. `application.properties` usa `sslmode=require` por defecto y `.env.example` lo desactiva para desarrollo local; el uso de SSL en el PostgreSQL de GCE queda como verificación del equipo.
- `sonar.exclusions` en `pom.xml` excluye `config`, `entities`, `security`, `services`, `DTOs` y `*Application` del análisis de SonarCloud. Por eso SonarCloud reporta 100 % de cobertura y complejidad 49 solo sobre controllers, excepciones, repositorios y utilidades, mientras JaCoCo mide todo el código. El análisis de `25c726b` no aparece en SonarCloud porque `main` se reescribió; la columna "Antes" de la sección 5 proviene de JaCoCo local.
