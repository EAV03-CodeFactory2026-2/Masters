# Hallazgos Sprint 2 — WebAPI Masters

Posibles defectos identificados durante la ampliación de pruebas unitarias (rama `feature/unit-tests-sprint2`). Ninguno fue corregido en código de producción y ningún test valida el comportamiento incorrecto como esperado.

| ID | Clase · método | Descripción | Impacto | Severidad | HU | Recomendación |
|---|---|---|---|---|---|---|
| H-01 | `NegocioService.crearNegocio` | La validación de identificación fiscal duplicada se ejecuta antes de verificar el rol Propietario. Un usuario sin el rol recibe 409 en lugar de 400. | Permite a un usuario no autorizado confirmar si un NIT ya está registrado (enumeración). Código HTTP inconsistente con la regla de negocio. | Media | Registro de negocios | Validar el rol antes de consultar el repositorio. |
| H-02 | `RolPerfilPropietarioStrategy.asignarPerfil` / `RolPerfilProveedorStrategy.asignarPerfil` | Si el request trae solo uno de los dos campos del documento, el faltante se completa con el del perfil existente. Puede combinar el tipo de documento del request con el número de otro perfil. | Perfil creado con un documento que no corresponde al usuario; afecta integridad de datos y la validación de duplicados. | Media | Usuarios/Perfiles/Roles | Reutilizar el documento del otro perfil solo si ambos campos vienen vacíos; si llega uno, exigir el otro. |
| H-03 | `NegocioController.crearNegocio` / `listarMisNegocios`, `ServicioController.crearServicio`, `UsuarioController.obtenerMiPerfil` | `Integer.parseInt(principal.getName())` sin validación. Un `name` no numérico produce `NumberFormatException`, que se captura como `IllegalArgumentException` y responde 400 con el mensaje crudo ("For input string…"). Con `principal` nulo se produce NPE (500). | Exposición de mensajes internos y códigos HTTP incorrectos ante tokens mal formados. Riesgo bajo mientras el filtro JWT garantice un id numérico. | Baja | Transversal (seguridad) | Obtener el id desde `IAuthenticationFacade` y responder 401 cuando no exista un usuario autenticado válido. |
| H-04 | `RolPerfil*Strategy.soporta` | No aplica `trim()` al nombre del rol, a diferencia de `Registro*Strategy.soporta`. `" Proveedor "` no es soportado. | Comportamiento inconsistente entre estrategias; hoy no se manifiesta porque `UsuarioService` envía el nombre tal como está en BD. | Baja | Usuarios/Perfiles/Roles | Unificar la normalización (`trim` + `equalsIgnoreCase`) en ambas familias de estrategias. |
| H-05 | `RegistroService.registrar` | Un rol inexistente (p. ej. "Administrador") devuelve el mismo mensaje que un rol vacío: "Debes indicar el rol con el que te registras." | El usuario no distingue entre rol omitido y rol inválido. | Baja | Usuarios/Perfiles/Roles | Mensaje específico para rol no soportado (p. ej. "El rol indicado no es válido"). |
| H-06 | `RecursoValidator.validarCantidad` | La expresión `[+-]?\d+` acepta cadenas con signo; `"+8"` se interpreta como 8. | Contradice el criterio de la HU ("solo dígitos"). Valores negativos se rechazan luego por rango, por lo que no hay corrupción de datos. | Baja | HU-0010 | Cambiar la expresión a `\d+` para entradas de tipo texto. |
| H-07 | `UsuarioService.listarPorRol` | Para el rol Propietario sin usuario autenticado retorna lista vacía, mientras `listarUsuarios` retorna todos los registros en la misma condición. | Inconsistencia de reglas de visibilidad; no alcanzable hoy porque el endpoint exige rol Propietario. | Baja | Usuarios/Perfiles/Roles | Unificar la política de visibilidad sin autenticación en ambos métodos. |
| H-08 | `HorarioValidator.parsearDia` | Con día nulo el mensaje se construye como "El día 'null' no es válido…". | Mensaje poco claro para el cliente de la API. | Baja | Horario Proveedores | Mensaje específico para día ausente (p. ej. "El día es obligatorio."). |

## Resumen

| Severidad | Cantidad |
|---|---|
| Alta | 0 |
| Media | 2 |
| Baja | 6 |
