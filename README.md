# Masters — API REST de Gestión de Maestros y Dominio

Microservicio independiente extraído del monolito modular `com.udea.Backend` (Spring Modulith).  
Administra las **entidades maestras del dominio** de la plataforma de reservas de servicios: usuarios, roles, monedas y modalidades de servicio.  
Paquete raíz: `com.udea.masters`

## Contexto en la arquitectura

Este servicio agrupa los módulos `Usuarios`, `Plataforma` (catálogos) del monolito original.  
Actúa como **fuente de verdad** para los datos de referencia que los demás servicios consumen mediante identificadores lógicos (no por FK directas):

- **WebAuth** consulta `usuario` / `rol` para autenticar y emitir el JWT.
- **Business** referencia `propietarioId`, `monedaCodigoIso` y `modalidadId` sin FK directas.

```
WebAuth  →  valida credenciales en Masters (tabla usuario/rol)
Business →  usa propietarioId / monedaCodigoIso / modalidadId (desacoplado)
Masters  →  administra usuarios, roles, monedas y modalidades
```

## Tech Stack

| Capa               | Tecnología                                                |
|--------------------|-----------------------------------------------------------|
| Lenguaje           | Java 21                                                   |
| Framework          | Spring Boot 4.1.1 (WebMVC)                                |
| Seguridad          | Spring Security + JJWT 0.12.5 (Resource Server / Stateless Bearer JWT HS256) |
| Base de datos      | PostgreSQL (Supabase) — Spring Data JPA / Hibernate       |
| Base datos pruebas | H2 in-memory (`MODE=PostgreSQL`)                          |
| Validación         | Jakarta Bean Validation                                   |
| Documentación API  | SpringDoc OpenAPI 2.6.0 (Swagger UI en `/swagger-ui.html`)|
| Boilerplate        | Lombok                                                    |
| Build              | Maven (wrapper: `mvnw` / `mvnw.cmd`)                      |
| Calidad & Cobertura| JaCoCo 0.8.11 + SonarCloud                               |
| Contenedores       | Docker multi-stage (eclipse-temurin:21)                   |
| Tests              | JUnit 5 + Mockito + Spring Boot Test                      |

## Estructura del proyecto

```
src/main/java/com/udea/masters/
├── MastersApplication.java              # @SpringBootApplication — punto de entrada
├── HealthController.java                # GET /api/v1/health
│
├── config/
│   ├── RolDataSeeder.java               # Siembra roles iniciales (@Order 1)
│   ├── UsuarioDataSeeder.java           # Siembra usuario admin inicial (@Order 2)
│   ├── ModalidadDataSeeder.java         # Siembra modalidades de servicio (@Order 1)
│   ├── MonedaDataSeeder.java            # Siembra catálogo de monedas
│   └── SwaggerConfig.java              # Configuración OpenAPI con soporte Bearer JWT
│
├── controllers/
│   ├── UsuarioController.java           # CRUD y gestión de roles de usuario
│   ├── MonedaController.java            # GET /api/v1/monedas
│   ├── ModalidadController.java         # GET /api/v1/modalidades
│   └── DTOs/
│       ├── UsuarioCreateRequest.java    # { nombre, email, password, ... }
│       ├── UsuarioResponse.java         # Proyección de usuario sin datos sensibles
│       └── RolAsignacionRequest.java    # { nombreRol }
│
├── entities/
│   ├── Usuario.java                     # id, nombre, email, passwordHash, activo
│   ├── Rol.java                         # id, nombre (TipoRol)
│   ├── TipoRol.java                     # Enum: CLIENTE, PROVEEDOR, PROPIETARIO
│   ├── Moneda.java                      # id, codigoIso, nombre, simbolo
│   ├── TipoMoneda.java                  # Enum: COP, USD, EUR, ...
│   ├── ModalidadServicio.java           # id, nombre (TipoModalidad)
│   └── TipoModalidad.java              # Enum: PRESENCIAL, VIRTUAL, HIBRIDO
│
├── repositories/
│   ├── UsuarioRepository.java           # findByEmail, findByRoles_Nombre
│   ├── RolRepository.java               # findByNombre
│   ├── MonedaRepository.java            # findByCodigoIso
│   └── ModalidadServicioRepository.java # findByNombre
│
├── security/
│   ├── IJwtService.java                 # Contrato: extractUserId, isTokenValid
│   ├── JwtService.java                  # Implementación HS256 · JJWT 0.12.5
│   ├── JwtAuthenticationFilter.java     # OncePerRequestFilter — valida Bearer en cada petición
│   └── SecurityConfig.java             # SecurityFilterChain STATELESS
│
└── services/
    ├── IUsuarioService.java             # Contrato de gestión de usuarios y roles
    └── UsuarioService.java             # Lógica: creación, consultas, asignación/remoción de roles
```

## API Endpoints

| Método   | Ruta                                         | Seguridad   | Descripción                                         |
|----------|----------------------------------------------|-------------|-----------------------------------------------------|
| `GET`    | `/api/v1/health`                             | Público     | Health check de la API                              |
| `GET`    | `/api/v1/monedas`                            | Bearer JWT  | Listado de monedas disponibles (COP, USD, EUR, …)  |
| `GET`    | `/api/v1/modalidades`                        | Bearer JWT  | Listado de modalidades (Presencial, Virtual, Híbrido) |
| `GET`    | `/api/v1/usuarios`                           | Bearer JWT  | Listado de todos los usuarios registrados           |
| `GET`    | `/api/v1/usuarios/{id}`                      | Bearer JWT  | Consulta de usuario por ID                          |
| `GET`    | `/api/v1/usuarios/rol/{nombreRol}`           | Bearer JWT  | Consulta de usuarios por rol (ej. `PROVEEDOR`)      |
| `POST`   | `/api/v1/usuarios`                           | Bearer JWT  | Creación de usuario maestro                         |
| `POST`   | `/api/v1/usuarios/{id}/roles`                | Bearer JWT  | Asignación de rol a un usuario                      |
| `GET`    | `/api/v1/proveedores/directorio?query=&pagina=` | Bearer JWT (Propietario) | Búsqueda paginada de proveedores activos (10 por página) |
| `GET`    | `/api/v1/proveedores/me/horario`             | Bearer JWT (rol activo Proveedor) | Horario semanal del proveedor autenticado (7 días) |
| `PUT`    | `/api/v1/proveedores/me/horario`             | Bearer JWT (rol activo Proveedor) | Reemplaza por completo el horario semanal |
| `POST`   | `/api/v1/negocios/{negocioId}/recursos`      | Bearer JWT (rol activo Propietario, dueño del negocio) | Registro de un recurso físico (espacio o equipo) |
| `GET`    | `/api/v1/negocios/{negocioId}/recursos`      | Bearer JWT (rol activo Propietario, dueño del negocio) | Listado de recursos del negocio (puede ser vacío) |
| `DELETE` | `/api/v1/usuarios/{id}/roles/{nombreRol}`    | Bearer JWT  | Remoción de rol de un usuario                       |
| `GET`    | `/swagger-ui.html`                           | Público     | Documentación interactiva Swagger UI                |
| `GET`    | `/v3/api-docs`                               | Público     | Especificación OpenAPI v3 en JSON                   |

### Ejemplo — Crear usuario

**Request**
```http
POST /api/v1/usuarios
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombre": "María López",
  "email": "maria@ejemplo.com",
  "password": "clave123"
}
```

**Response 201 Created**
```json
{
  "idUsuario": 10,
  "nombre": "María López",
  "email": "maria@ejemplo.com",
  "activo": true,
  "roles": []
}
```

### Ejemplo — Asignar rol

**Request**
```http
POST /api/v1/usuarios/10/roles
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "nombreRol": "PROPIETARIO" }
```

**Response 200 OK**
```json
{
  "idUsuario": 10,
  "nombre": "María López",
  "roles": ["PROPIETARIO"]
}
```

### Ejemplo — Buscar en el directorio de proveedores

```http
GET /api/v1/proveedores/directorio?query=Carlos%20Ruiz&pagina=1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Busca por nombre, apellido o especialidad (coincidencia parcial) o por número de documento (exacto), sin distinguir mayúsculas. Solo devuelve usuarios activos con el rol `Proveedor`. Cada resultado incluye el tipo y número de documento para diferenciar homónimos, y `esCuentaActual` indica si es la cuenta que consulta (un Propietario que también es Proveedor puede asignarse servicios). Los `idUsuario` son los que se envían a Business para asignar proveedores a un servicio.

```json
{
  "mensaje": "Proveedores encontrados.",
  "proveedores": [
    {
      "idUsuario": 11,
      "nombre": "Carlos",
      "apellido": "Ruiz",
      "correo": "carlos.ruiz@correo.com",
      "telefono": "3001234567",
      "tipoDocumento": "CC",
      "tipoDocumentoNombre": "Cédula de Ciudadanía",
      "numeroDocumento": "123456",
      "especialidad": "Estilista",
      "descripcionProfesional": "Cortes y peinados",
      "esCuentaActual": false
    }
  ],
  "pagina": 1,
  "tamanoPagina": 10,
  "totalResultados": 1,
  "totalPaginas": 1,
  "hayMasPaginas": false
}
```

| Código | Cuándo | `error` / `mensaje` |
|--------|--------|---------|
| `200` | Sin coincidencias | `mensaje`: `No encontramos proveedores con ese criterio.` |
| `400` | Criterio con menos de 3 caracteres | `Escribe al menos 3 caracteres para buscar.` |
| `400` | Página menor a 1 | `La página debe ser un número mayor o igual a 1.` |
| `403` | El usuario no tiene rol Propietario | — |

### Ejemplo — Horario de atención del proveedor

El proveedor autenticado consulta y guarda su horario semanal recurrente. `GET` siempre devuelve los 7 días, de lunes a domingo; si nunca se ha guardado un horario, todos figuran inactivos y `fechaActualizacion` es `null`.

**Request**
```http
PUT /api/v1/proveedores/me/horario
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "dias": [
    { "dia": "LUNES",  "activo": true, "bloques": [ { "horaInicio": "08:00", "horaFin": "12:00" }, { "horaInicio": "14:00", "horaFin": "18:00" } ] },
    { "dia": "SABADO", "activo": true, "bloques": [ { "horaInicio": "08:00", "horaFin": "12:00" } ] }
  ]
}
```

**Response 200 OK**
```json
{
  "mensaje": "Horario guardado correctamente.",
  "fechaActualizacion": "2026-10-14T10:30:00-05:00",
  "dias": [
    { "dia": "LUNES", "nombre": "Lunes", "activo": true, "bloques": [ { "horaInicio": "08:00", "horaFin": "12:00" }, { "horaInicio": "14:00", "horaFin": "18:00" } ] },
    { "dia": "MARTES", "nombre": "Martes", "activo": false, "bloques": [] },
    "...",
    { "dia": "DOMINGO", "nombre": "Domingo", "activo": false, "bloques": [] }
  ]
}
```

Reglas:
- Guardar **reemplaza por completo** el horario anterior: los días que no se envían quedan inactivos.
- Un día con `"activo": false` se guarda inactivo y sus bloques se ignoran. Si se omite `activo`, el día está activo cuando trae bloques.
- Cada día activo admite de 1 a 4 bloques, en formato `HH:mm` de 24 horas y en intervalos de 15 minutos.
- Los bloques de un mismo día no pueden superponerse; los contiguos (`08:00-12:00` y `12:00-14:00`) sí se permiten. No se admiten franjas que crucen la medianoche.
- Si una regla falla, no se guarda nada y el horario anterior se conserva.

| Código | Cuándo | `error` |
|--------|--------|---------|
| `400` | Ningún día activo | `Debes definir al menos un día de atención.` |
| `400` | Hora de fin igual o anterior a la de inicio | `La hora de fin debe ser posterior a la hora de inicio.` (incluye `dia` y `bloque`) |
| `400` | Bloques superpuestos | `Los bloques del miércoles se superponen.` (incluye `dia` y `bloque`) |
| `400` | Más de 4 bloques en un día | `Cada día admite un máximo de 4 bloques.` (incluye `dia`) |
| `400` | Hora que no es múltiplo de 15 minutos | `Las horas deben estar en intervalos de 15 minutos.` (incluye `dia` y `bloque`) |
| `400` | Hora con formato inválido | `Las horas deben tener el formato HH:mm de 24 horas (ej. 08:00).` |
| `403` | La cuenta no opera con el rol activo Proveedor | `Solo un Proveedor puede gestionar su horario de atención.` |
| `404` | La cuenta no tiene perfil de proveedor | `No encontramos el perfil de proveedor de tu cuenta.` |

> **Rol activo**: si el JWT trae el claim `rolActivo` (lo emite WebAuth), el filtro de seguridad solo concede ese rol, y solo si está en `roles`. Así, una cuenta Propietario + Proveedor que opera como Propietario no puede gestionar el horario. Si el token no trae `rolActivo`, se conservan todos los roles.

### Ejemplo — Registrar recurso físico

**Request**
```http
POST /api/v1/negocios/5/recursos
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombre": "Camilla de masajes",
  "tipo": "Equipo",
  "cantidad": 3,
  "descripcion": "Camilla plegable"
}
```

**Response 201 Created**
```json
{
  "mensaje": "Recurso registrado correctamente.",
  "recurso": {
    "idRecurso": 1,
    "negocioId": 5,
    "nombre": "Camilla de masajes",
    "tipo": "Equipo",
    "cantidad": 3,
    "descripcion": "Camilla plegable",
    "estado": "Activo"
  }
}
```

| Código | Cuándo | `error` |
|--------|--------|---------|
| `400` | Cantidad no entera o fuera de 1..999 | `La cantidad debe ser un número entero entre 1 y 999.` |
| `400` | Nombre vacío / tipo distinto de `Espacio` o `Equipo` | Mensaje de la validación |
| `403` | El rol activo no es Propietario | `Solo un Propietario puede gestionar los recursos del negocio.` |
| `403` | El negocio es de otro propietario | `No tienes permiso para gestionar los recursos de este negocio.` |
| `404` | El negocio no existe | `Negocio no encontrado.` |
| `409` | Nombre repetido en el negocio (sin distinguir mayúsculas ni tildes) | `Ya existe un recurso con este nombre en el negocio.` |

`GET /api/v1/negocios/5/recursos` retorna `{"mensaje": ..., "recursos": [...]}`; si no hay recursos, `recursos` es `[]` y el mensaje es `Este negocio aún no tiene recursos registrados.`

> El nombre es único por negocio sin distinguir mayúsculas, tildes ni espacios extra (columna `nombre_normalizado` con restricción única `negocio_id, nombre_normalizado`); se permite repetir en otro negocio. Masters lee la tabla `negocio` como entidad de **solo lectura** (`@Immutable`), únicamente para validar al propietario. Los recursos usan la misma tabla `recurso` que antes estaba en Business, así que los datos existentes se conservan.

## Datos de referencia sembrados al iniciar

### Roles (`RolDataSeeder` — @Order 1)

| Nombre       | Descripción                                 |
|--------------|---------------------------------------------|
| `CLIENTE`    | Usuario que reserva servicios               |
| `PROVEEDOR`  | Usuario que presta servicios                |
| `PROPIETARIO`| Usuario que registra y gestiona negocios    |

### Monedas (`MonedaDataSeeder`)

COP · USD · EUR · GBP · JPY · BRL · MXN · ARS · CLP · PEN

### Modalidades de servicio (`ModalidadDataSeeder` — @Order 1)

| Nombre       | Descripción                                     |
|--------------|-------------------------------------------------|
| `PRESENCIAL` | El servicio se presta en las instalaciones físicas |
| `VIRTUAL`    | El servicio se presta de forma remota/en línea  |
| `HIBRIDO`    | Combinación de presencial y virtual             |

### Usuario administrador (`UsuarioDataSeeder` — @Order 2)

Creado con las credenciales definidas en `ADMIN_EMAIL` y `ADMIN_PASSWORD`. Se le asigna el rol `PROPIETARIO` automáticamente.

## Configuración y variables de entorno

Copia `.env.example` como `.env` en la raíz del proyecto y completa los valores:

```bash
cp .env.example .env
```

| Variable            | Descripción                              | Valor por defecto / Ejemplo                             |
|---------------------|------------------------------------------|---------------------------------------------------------|
| `DB_HOST`           | Host de PostgreSQL (Supabase)            | `aws-0-...pooler.supabase.com`                          |
| `DB_PORT`           | Puerto de base de datos                  | `5432`                                                  |
| `DB_NAME`           | Nombre de la base de datos               | `postgres`                                              |
| `DB_USERNAME`       | Usuario de base de datos                 | `postgres.xxx`                                          |
| `DB_PASSWORD`       | Contraseña de base de datos              | `*****`                                                 |
| `DB_SSLMODE`        | Modo SSL                                 | `require`                                               |
| `JPA_DDL_AUTO`      | Estrategia DDL de Hibernate              | `update`                                                |
| `JWT_SECRET`        | Clave secreta Base64 (mín. 256 bits)     | `*****`                                                 |
| `JWT_EXPIRATION_MS` | Tiempo de vida del JWT en ms             | `86400000` (24 horas)                                   |
| `ADMIN_EMAIL`       | Correo del usuario administrador inicial | `admin@udea.edu.co`                                     |
| `ADMIN_PASSWORD`    | Contraseña del administrador inicial     | `*****`                                                 |
| `PORT`              | Puerto HTTP del servidor                 | `8082`                                                  |

> **⚠️ IMPORTANTE**: Jamás commitear secretos en texto plano. En producción (Render, CI) las variables se inyectan en el entorno.  
> La clave `JWT_SECRET` debe ser **idéntica** en todos los servicios del ecosistema.

## Comandos de desarrollo

```bash
# Compilar sin tests
./mvnw clean compile          # Linux / macOS
mvnw.cmd clean compile        # Windows

# Ejecutar la aplicación (puerto 8082 por defecto)
./mvnw spring-boot:run
mvnw.cmd spring-boot:run      # Windows

# Empaquetar JAR (sin tests)
./mvnw clean package -DskipTests

# Ejecutar todos los tests (con reporte JaCoCo)
./mvnw test
mvnw.cmd test                 # Windows

# Ejecutar un test específico
./mvnw test -Dtest=UsuarioServiceTest

# Con output detallado en consola
./mvnw test -Dsurefire.useFile=false

# Docker build & run
docker build -t masters .
docker run -p 8082:8082 --env-file .env masters
```

## Testing

- **JUnit 5** + **Mockito** (`@ExtendWith(MockitoExtension.class)`)
- **H2 en memoria** configurado en `src/test/resources/application.properties` con modo PostgreSQL (`create-drop`), garantizando tests herméticos.
- Clases de test ubicadas en `src/test/java/com/udea/masters/` replicando la jerarquía de paquetes de producción.
- Convenciones de nombres: sufijo `Test` (ej. `UsuarioServiceTest`, `UsuarioControllerTest`); métodos en camelCase descriptivo sin prefijo `test` (ej. `crearUsuarioExitoso`, `debeRetornar404CuandoUsuarioNoExiste`).
- Smoke test de contexto (`MastersApplicationTests#contextLoads`) siempre presente.

## Convenciones de código

- **Paquetes**: camelCase (`config`, `controllers`, `entities`, `repositories`, `security`, `services`).
- **Clases**: PascalCase. Interfaces de servicio con prefijo `I` (`IUsuarioService`, `IJwtService`).
- **Métodos y variables**: camelCase. Constantes: `UPPER_SNAKE_CASE`.
- **Lombok**: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en entidades y DTOs.
- **Idioma**: español para nombres de dominio, Javadoc y mensajes de error.
- Endpoints documentados con `@Operation`, `@ApiResponse` y `@Tag` (SpringDoc/OpenAPI).
- Validación de entrada estricta con Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Email`) en DTOs bajo `controllers/DTOs`.
