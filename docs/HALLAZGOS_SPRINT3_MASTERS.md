# Hallazgos de calidad — Sprint 3 — Masters

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | WebAPI Masters |
| Sprint | 3 |
| Tipo de verificación | Verificación de cierre del Sprint 3 (HU-0046 y HU-0044) |
| Fecha de verificación | 2026-11-20 |
| Rama y commit verificados | `qa/sprint3` @ `8da54c9` (`8da54c967ee969f1f8d644e8d327568d668e3a6a`) |
| Línea base | `main` @ `8f7d9df` (`8f7d9df2023aa720550dcf30eb1b9d54d15e1bfd`) |
| Tests ejecutados | 518 (antes) → 584 (después) (+66) |
| Fallos | 0 |
| Cobertura de líneas (alcance Sonar) | 77,28 % (711/920) → 93,26 % (858/920) |
| Cobertura de ramas (alcance Sonar) | 65,43 % (246/376) → 95,74 % (360/376) |
| Cobertura estilo Sonar (líneas + ramas, alcance Sonar) | 73,84 % (957/1296) → 93,98 % (1218/1296) |
| Cobertura de líneas (JaCoCo, todo el código) | 79,32 % (836/1054) → 93,26 % (983/1054) |
| Cobertura de ramas (JaCoCo, todo el código) | 64,68 % (260/402) → 93,03 % (374/402) |
| SonarCloud (`main` @ `8f7d9df`) | Quality Gate SonarCloud: OK (`new_coverage` 85,7 %) · Gate workflow (`build.yml`): falla condición `complexity < 50` (52 ≥ 50) · cobertura 95,6 % (líneas 94,7 %, ramas 100,0 %) · complejidad del proyecto 52 · complejidad máxima por clase 11 (`GlobalExceptionHandler`) · deuda 0 min · 0 bugs · 0 vulnerabilidades |
| SonarCloud (`qa/sprint3` @ `8da54c9`) | No disponible (sin análisis de `qa/sprint3`) |

> Nota sobre el alcance Sonar: se calcula aplicando las mismas exclusiones de cobertura del workflow (`build.yml`): `**/config/**`, `**/entities/**`, `**/*DataSeeder.java` y `**/*Application.java`. En SonarCloud, `sonar.exclusions` de `pom.xml` excluye además `security`, `services` y `DTOs`.

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Cerrados por decisión | Total |
|---|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 | 0 |
| Media | 3 | 0 | 0 | 0 | 3 |
| Baja | 0 | 0 | 0 | 0 | 0 |
| Por confirmar | 0 | 0 | 0 | 0 | 0 |
| Total | 3 | 0 | 0 | 0 | 3 |

> Nota sobre el conteo: los 3 hallazgos vigentes de severidad Media corresponden a 1 hallazgo nuevo del Sprint 3 (`H-01`) y 2 hallazgos heredados vigentes del Sprint 2 (`H-09`, reverificado en el código actual dentro del alcance de `HU-0044`, y `H-10`, vigente pero fuera del alcance de las HU del Sprint 3 y no reverificado). Los 8 hallazgos corregidos durante el Sprint 2 (`H-01` a `H-08` del informe anterior) cerraron en ese sprint y no se contabilizan en esta tabla.

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CA | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | Nuevo | `ServicioService.listarServiciosPorNegocio` (L65-L67) / `ServicioController.listarServiciosPorNegocio` (L72-L75) | `ServicioService.listarServiciosPorNegocio` (L65-L67) retorna directamente `servicioRepository.findByNegocioIdNegocio(negocioId)` sin consultar `NegocioRepository` (a pesar de tener `negocioRepository` inyectado en L24), y `ServicioController.listarServiciosPorNegocio` (L72-L75) responde siempre con `ResponseEntity.ok(...)`. Entrada que lo reproduce: `GET /api/v1/servicios/negocio/999` con un `negocioId = 999` inexistente responde HTTP `200 OK` con `[]` en lugar de HTTP `404 Not Found` informando que el negocio no fue encontrado. | Contradice el criterio de aceptación `HU-0044/CA-05`: el cliente o consumidor de la API no puede distinguir entre un negocio activo sin servicios (`CA-04`) y un negocio inexistente (`CA-05`). | Media | HU-0044/CA-05 | Vigente (nuevo) | Verificar en `ServicioService.listarServiciosPorNegocio` la existencia del negocio en `NegocioRepository` y lanzar `RecursoNoEncontradoException("Negocio no encontrado.")` para que `GlobalExceptionHandler` responda HTTP 404. |
| H-09 | Sprint 2 | `NegocioController.listarNegociosDisponibles` (L93-L96) / `NegocioService.listarNegociosDisponibles` (L72-L74) / `ServicioController.listarServiciosPorNegocio` (L72-L75) / `Negocio` (L22-L23, L34-L35) / `Servicio` (L24-L26) | `GET /api/v1/negocios/disponibles` (`NegocioController` L93-L96) sigue devolviendo `List<Negocio>` directamente; en la entidad `Negocio` los campos `propietarioId` (L22-L23) e `identificacionFiscal` (L34-L35) no tienen `@JsonIgnore`. Además, `GET /api/v1/servicios/negocio/{id}` (`ServicioController` L72-L75) devuelve `List<Servicio>` sin DTO intermedio y en `Servicio` la asociación `@ManyToOne(fetch = FetchType.LAZY) private Negocio negocio` (L24-L26) no tiene `@JsonIgnore` ni `@JsonBackReference`. Evidencia verificada con `@SpringBootTest` sobre H2 (`jacksonJsonMapper` / `tools.jackson.databind.json.JsonMapper` y `JacksonJsonHttpMessageConverter`): al limpiar el `EntityManager`, consultar `servicioRepository.findByNegocioIdNegocio(negocioId)` dentro de una transacción y serializar la lista, Jackson 3 (que deshabilita `FAIL_ON_EMPTY_BEANS` por defecto) inicializa el proxy `Negocio$HibernateProxy` con un `SELECT ... FROM negocio WHERE id_negocio=?` y produce `[{"idServicio":1,"negocio":{"idNegocio":1,"propietarioId":77,"nombreNegocio":"Barbería Repro",...,"identificacionFiscal":"NIT-REPRO-999","monedaCodigoIso":"COP","estado":"Activo","hibernateLazyInitializer":{}},...}]`. | Cualquier usuario con rol Cliente obtiene la identificación fiscal (`identificacionFiscal`) y el identificador interno del propietario (`propietarioId`) de los negocios tanto al consultar el catálogo de negocios como al listar los servicios de un negocio (junto con el artefacto `"hibernateLazyInitializer":{}` del proxy), acoplando el contrato de la API al modelo de persistencia. | Media | HU-0044/CA-01, HU-0044/CA-03 | Vigente (heredado del Sprint 2) | Responder con DTOs de catálogo para negocios y servicios que omitan `identificacionFiscal`, `propietarioId` y la entidad `Negocio` completa. |

Los demás hallazgos del Sprint 2 (`H-10`): vigentes, fuera del alcance de las HU del Sprint 3, no reverificados.

## 4. Pendientes de definir con el PO

No aplica.

## 5. Escenarios no verificables con pruebas unitarias

Los siguientes escenarios dependen del filtro de seguridad o de endpoints fuera del controlador unitario y quedan para las pruebas E2E del equipo:

| HU / CA | Escenario | Motivo |
|---|---|---|
| HU-0044/CA-06 | Impedir consultas a usuarios no autenticados (`HTTP 401` en `GET /api/v1/negocios/disponibles` sin JWT válido) | `NegocioController.listarNegociosDisponibles` (L93-L96) delega la validación de autenticación a la cadena de filtros de Spring Security (`SecurityConfig` L54-L58, `.anyRequest().authenticated()` con `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`), la cual no se ejecuta en pruebas unitarias de controlador con `MockMvc` en modo standalone. |
| HU-0044/CA-07 | Restringir las operaciones según el rol Cliente (`HTTP 403` cuando un cliente intenta modificar un negocio) | No existe endpoint de edición (`PUT`/`PATCH`) de negocios en `NegocioController`. En el registro (`POST /api/v1/negocios`), la restricción de rol se valida en `NegocioService.crearNegocio` lanzando `IllegalArgumentException`, que `NegocioController` (L54-L56) traduce a `HTTP 400 Bad Request` (y `SecurityConfig` no declara `.hasRole("PROPIETARIO")` sobre `/api/v1/negocios`). |

## 6. Complejidad ciclomática

| Métrica | Antes (`main` @ `8f7d9df`) | Después (`qa/sprint3` @ `8da54c9`) |
|---|---|---|
| Complejidad del proyecto (JaCoCo, alcance de cobertura Sonar) | 382 | 382 |
| Complejidad del proyecto (JaCoCo, todo el código) | 436 | 436 |
| Complejidad con `sonar.exclusions` de `pom.xml` (JaCoCo) | 53 | 53 |
| Complejidad del proyecto (SonarCloud) | 52 | No disponible (sin análisis de `qa/sprint3`) |
| Complejidad máxima por clase (JaCoCo, alcance de cobertura Sonar) | 61 (`FranjaServicioService`) | 61 (`FranjaServicioService`) |
| Complejidad máxima por clase (SonarCloud, con `sonar.exclusions`) | 11 (`GlobalExceptionHandler`) | No disponible (sin análisis de `qa/sprint3`) |
| Cobertura en SonarCloud | 95,6 % (líneas 94,7 %, ramas 100,0 %) | No disponible (sin análisis de `qa/sprint3`) |
| Quality Gate (SonarCloud / workflow `build.yml`) | SonarCloud: OK · Workflow: No cumple (`complexity` 52 ≥ 50) | No disponible (sin análisis de `qa/sprint3`) |

### Top 5 de complejidad por clase (JaCoCo, con exclusiones de cobertura Sonar)

| # | Clase | Complejidad | Líneas | Ramas |
|---|---|---|---|---|
| 1 | `FranjaServicioService` | 61 | 114/114 (100,0 %) | 90/90 (100,0 %) |
| 2 | `UsuarioService` | 31 | 82/82 (100,0 %) | 22/22 (100,0 %) |
| 3 | `HorarioValidator` | 28 | 64/64 (100,0 %) | 28/28 (100,0 %) |
| 4 | `RegistroProveedorStrategy` | 21 | 38/38 (100,0 %) | 30/30 (100,0 %) |
| 5 | `RegistroPropietarioStrategy` / `RolPerfilProveedorStrategy` | 17 | 32/32 · 40/40 (100,0 %) | 22/22 · 24/24 (100,0 %) |

| Criterio | Antes (`main` @ `8f7d9df`) | Después (`qa/sprint3` @ `8da54c9`) |
|---|---|---|
| Por proyecto en SonarCloud (`< 50`, workflow `build.yml`) | No cumple (52) | No disponible (sin análisis de `qa/sprint3`) |
| Por proyecto en JaCoCo, alcance de cobertura Sonar (`< 50`) | No cumple (382) | No cumple (382) |
| Por clase en JaCoCo (`< 50`) | No cumple (61, `FranjaServicioService`) | No cumple (61, `FranjaServicioService`) |
| Por clase en SonarCloud (`< 50`, con `sonar.exclusions`) | Cumple (11, `GlobalExceptionHandler`) | No disponible (sin análisis de `qa/sprint3`) |

> Nota: el criterio `complexity < 50` del workflow no se cumple (en SonarCloud sobre `main` @ `8f7d9df` el acumulado es 52 por los 3 puntos de `FranjaServicioController`, y en JaCoCo `FranjaServicioService` suma 61 por sí sola). De acuerdo con las reglas del proyecto (`CLAUDE.md` §3.1), la reducción de complejidad ciclomática no se registra como hallazgo de QA y la gestiona el equipo de Arquitectura.

## 7. Cobertura por clase del alcance

| Ámbito / HU | Clase | Líneas antes | Líneas después | % Líneas | Ramas antes | Ramas después | % Ramas |
|---|---|---|---|---|---|---|---|
| HU-0046 | `FranjaServicioService` | 1/114 (0,9 %) | 114/114 | 100,0 % | 0/90 (0,0 %) | 90/90 | 100,0 % |
| HU-0046 | `FranjaServicioResponse` | 0/28 (0,0 %) | 28/28 | 100,0 % | 0/24 (0,0 %) | 24/24 | 100,0 % |
| HU-0046 | `FranjaServicioController` | 0/6 (0,0 %) | 6/6 | 100,0 % | 0/0 (s/r) | 0/0 | s/r |
| HU-0044 | `NegocioService` | 26/26 (100,0 %) | 26/26 | 100,0 % | 10/10 (100,0 %) | 10/10 | 100,0 % |
| HU-0044 | `NegocioController` | 22/22 (100,0 %) | 22/22 | 100,0 % | 4/4 (100,0 %) | 4/4 | 100,0 % |
| HU-0044 | `ServicioService` | 26/26 (100,0 %) | 26/26 | 100,0 % | 10/10 (100,0 %) | 10/10 | 100,0 % |
| HU-0044 | `ServicioController` | 16/16 (100,0 %) | 16/16 | 100,0 % | 2/2 (100,0 %) | 2/2 | 100,0 % |
| Transversal | `TextoNormalizador` | 5/5 (100,0 %) | 5/5 | 100,0 % | 2/2 (100,0 %) | 2/2 | 100,0 % |
| Transversal (`security/*`) | `SecurityConfig` | 35/35 (100,0 %) | 35/35 | 100,0 % | 4/4 (100,0 %) | 4/4 | 100,0 % |
| Transversal (`security/*`) | `JwtService` | 4/28 (14,3 %) | 4/28 | 14,3 % | 0/2 (0,0 %) | 0/2 | 0,0 % |
| Transversal (`security/*`) | `JwtAuthenticationFilter` | 0/24 (0,0 %) | 0/24 | 0,0 % | 0/10 (0,0 %) | 0/10 | 0,0 % |
| Transversal (`security/*`) | `AuthenticationFacade` | 1/15 (6,7 %) | 1/15 | 6,7 % | 0/4 (0,0 %) | 0/4 | 0,0 % |
