# Fase 4 — JWT, autorización, SLA, escalamiento, métricas y calidad

## JWT y flujo de autenticación

1. El cliente envía `POST /auth/login` con email y contraseña.
2. `AuthenticateUseCaseImpl` busca el usuario por email, verifica el hash BCrypt con `PasswordHasherPort`, y verifica que el usuario esté ACTIVO.
3. Si todo pasa, `JwtTokenIssuerAdapter.issue()` genera un JWT firmado con HS256.
4. El JWT contiene claims: `sub` (user id), `email`, `role`, `iat`, `exp`, `jti`.
5. El secreto viene de la variable de entorno `JWT_SECRET` (mín 32 bytes).
6. En cada petición subsiguiente, `JwtAuthenticationFilter` valida la firma y expiración del JWT, carga el usuario del repositorio para verificar que siga ACTIVO, y pobla el `SecurityContext` con el userId y el rol.
7. Si el token falta o es inválido, el `CustomAuthenticationEntryPoint` devuelve 401 con ProblemDetail.
8. `SessionCreationPolicy.STATELESS` (sin sesión), CSRF deshabilitado (API stateless).

## Roles y autorización por recurso

- **Coarse-grained** con `@PreAuthorize` en los controllers:
  - `/admin/**` → `hasRole('ADMIN')`
  - `GET /users`, `GET /users/{id}`, `PUT /users/{id}`, `PATCH /users/{id}/status` → `hasRole('ADMIN')`
  - `POST /technicians`, `GET /technicians`, etc. → `hasRole('ADMIN')`
  - `GET /technicians/me` → `hasRole('TECNICO')`
- **Fine-grained** en los casos de uso (ownership/assignment):
  - `GetTicketsUseCaseImpl.checkAccess()` verifica que el USUARIO acceda solo a sus tickets, el TECNICO a los asignados, el ADMIN a todo. Si no → `ForbiddenOperationException` → 403.
  - Esto previene IDOR: conocer un ID no da acceso. El check se hace contra datos persistidos, no contra datos de la petición.
- **401 vs 403**: 401 cuando no hay token o es inválido; 403 cuando el usuario está autenticado pero no tiene permisos.

## Cómo se calcula el SLA

`SlaCalculator.computeDeadline()` lee las políticas SLA de la BD (tabla `sla_policies`) y calcula:
```
slaDeadline = createdAt + resolutionHours(priority) * 3600 segundos
```
Valores iniciales (V3): CRITICA 2h, ALTA 4h, MEDIA 8h, BAJA 24h. El admin puede cambiarlos con `PUT /admin/sla-policies` con validación cruzada (CRITICA < ALTA < MEDIA < BAJA).

## Diferencia entre vencimiento y escalamiento

- **Vencimiento** (`isOverdue`): el ticket superó su `slaDeadline` y sigue en estado abierto (ABIERTO/ASIGNADO/EN_PROGRESO). Es un estado actual, consultado on-demand.
- **Escalamiento** (`escalate`): acción preventiva cuando el tiempo transcurrido llega al 80% de la ventana SLA. Sube la prioridad un nivel, marca `escalatedAt`, y si sigue ABIERTO reintenta la asignación. Ocurre máximo una vez por ticket (idempotente).
- Un ticket puede estar vencido sin haber sido escalado, y viceversa.

## Actuator, métricas y logs

- `Actuator` expone: `health`, `info` (públicos), `metrics`, `prometheus` (ROLE_ADMIN).
- Métricas con Micrometer (adaptador `MicrometerTicketMetricsAdapter`):
  - `helpflow.tickets.created{category,priority}` — counter
  - `helpflow.tickets.first_response` — timer
  - `helpflow.tickets.resolution` — timer
  - `helpflow.tickets.overdue` — gauge (count actual, refrescado por el scheduler)
  - `helpflow.sla.breaches` — counter
  - `helpflow.tickets.escalated` — counter
  - `helpflow.auth.failures{reason}` — counter
  - `helpflow.access.denied` — counter
  - `helpflow.dispatch.failures` — counter
- La aplicación reporta eventos a través de `TicketMetricsPort` (puerto). Micrometer nunca entra en el dominio.
- Logs estructurados en formato ECS con `traceId` en MDC (vía `TraceIdFilter`). Nunca se loguean contraseñas ni tokens completos.

## Admin bootstrap

`AdminBootstrapper` se ejecuta al iniciar la aplicación. Si no existe un admin, lo crea usando las variables de entorno `ADMIN_DOCUMENT`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`. Si la contraseña no está configurada, registra un warning y no crea el admin.

## Pruebas de seguridad y cobertura

- Test unitario de `JwtTokenIssuerAdapter`: verifica claims, firma, expiración.
- 79 tests totales (0 failures, 6 skipped por Testcontainers).
- Cobertura actual (sin gate): domain/service 89%, application/usecase ~40% (mejorará con integration tests).

## No verificado

- Integration tests con Testcontainers (Docker 29.x no detectado). Tests `@Disabled`.
- Cobertura >= 80%: se reactivará el gate cuando los integration tests funcionen.
