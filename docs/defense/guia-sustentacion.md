# Guía de Sustentación — HelpFlow

## Explicación del proyecto en 60 segundos

HelpFlow es un sistema de tickets de soporte TI con dos microservicios. El servicio principal gestiona usuarios, técnicos, tickets, SLA y seguridad JWT usando arquitectura hexagonal: el dominio no depende de Spring ni JPA, los casos de uso orquestan el dominio a través de puertos, y los adaptadores implementan esos puertos con tecnología real. El servicio de despacho es externo, sin base de datos, y sugiere técnicos de forma determinista usando CRC32. Los tickets se asignan automáticamente, se escalan al 80% del SLA, y todo se audita con historial.

## Arquitectura y razones de diseño

**¿Por qué hexagonal?** Separa el negocio de la tecnología. Si cambio PostgreSQL por MongoDB, el dominio no se toca. Si cambio JWT por OAuth2, solo se modifica el adaptador de seguridad. El dominio se prueba sin contexto de Spring ni base de datos.

**¿Por qué dos transacciones (TX-1 y TX-2)?** La llamada HTTP al dispatch service no debe mantener una transacción abierta. Si TX-1 ya hizo commit, el ticket no se pierde aunque el dispatch falle.

**¿Por qué CRC32 y no Random?** Determinismo. La misma entrada siempre produce la misma salida. `hashCode()` no es estable entre JVMs; `Random` no es reproducible sin semilla fija.

## Recorrido de una petición real: POST /tickets

1. `TraceIdFilter` genera/lee `X-Trace-Id`, lo pone en MDC.
2. `JwtAuthenticationFilter` valida el Bearer token, carga el usuario, verifica ACTIVO.
3. `TicketController.create()` recibe el DTO validado, resuelve el `AuthenticatedActor` del SecurityContext.
4. `CreateTicketUseCaseImpl.create()` orquesta:
   - `CreateTicketTxAdapter` (TX-1): valida solicitante, crea ticket ABIERTO + SLA + historial CREATED.
   - `RestClientDispatchAdapter.suggest()`: llama al dispatch service.
   - `AssignTicketTxAdapter` (TX-2): evalúa elegibilidad, asigna o deja ABIERTO.
5. Response 200 con el ticket (estado ABIERTO o ASIGNADO).

## Responsabilidad de paquetes principales

| Paquete | Responsabilidad |
|---|---|
| `domain/model` | Modelos ricos: `Ticket.create()`, `ticket.transitionTo()`, `ticket.escalate()` |
| `domain/service` | Políticas: `TicketTransitionPolicy` (matriz de estados), `SlaCalculator`, `TechnicianSelectionPolicy`, `EscalationPolicy` |
| `application/port/in` | Interfaces de casos de uso (contrato que el REST cumple) |
| `application/port/out` | Interfaces de dependencias externas (repositorios, dispatch, métricas) |
| `application/usecase` | Implementaciones que orquestan el dominio |
| `infrastructure/adapter/in/rest` | Controladores, DTOs, ApiExceptionHandler |
| `infrastructure/adapter/out/persistence` | Entidades JPA, repos, mappers, adaptadores |
| `infrastructure/adapter/out/dispatch` | RestClient para el dispatch service |
| `infrastructure/adapter/out/metrics` | Micrometer |
| `infrastructure/security` | JWT filter, SecurityConfig, BCrypt, handlers 401/403 |

## Preguntas técnicas probables

**1. ¿Cómo evitas que un usuario vea tickets de otros?**
En `GetTicketsUseCaseImpl.checkAccess()`: el USUARIO solo pasa si `ticket.getRequesterId().equals(actor.userId())`. El check se hace contra datos persistidos, no contra el body de la petición. Si falla, lanza `ForbiddenOperationException` → 403.

**2. ¿Qué pasa si el dispatch service está caído?**
`RestClientDispatchAdapter.suggest()` lanza `DispatchServiceException`. El `CreateTicketUseCaseImpl` la captura, registra la métrica, y `AssignTicketTxAdapter` recibe `null` como sugerencia. Intenta el fallback entre técnicos elegibles. Si tampoco hay, el ticket queda ABIERTO con historial `ASSIGNMENT_FAILED`. El ticket nunca se pierde.

**3. ¿Cómo funciona el escalamiento?**
`EscalationScheduler` corre cada 60s (configurable). Llama a `EscalateTicketsUseCaseImpl.escalate()` que usa `EscalationPolicy.findEligible()` para filtrar tickets al 80% del SLA, no escalados, en estado abierto. `Ticket.escalate()` sube la prioridad un nivel y marca `escalatedAt`. Es idempotente: si `escalatedAt` no es null, no hace nada.

**4. ¿Por qué el dominio no usa `@Entity`?**
Porque JPA es infraestructura. Si el dominio importara `jakarta.persistence.Entity`, no podría probarse sin base de datos y estaría acoplado a JPA. ArchUnit lo verifica: los tests fallan si alguien lo hace.

**5. ¿Qué es el `@Version` en el ticket?**
Bloqueo optimista. Si dos usuarios intentan cambiar el mismo ticket a la vez, Hibernate lanza `OptimisticLockException`. Previene actualizaciones perdidas sin necesidad de locks pesimistas en la mayoría de casos.

**6. ¿Cómo se diferencia vencimiento de escalamiento?**
Vencimiento = `slaDeadline < now` y estado abierto (estado actual, gauge). Escalamiento = acción preventiva al 80% del SLA (evento, counter). Un ticket puede estar escalado sin estar vencido, y viceversa.

## Errores encontrados y cómo se resolvieron

- **Multi-catch con subclases**: `catch (DispatchServiceException | RuntimeException)` no compila porque `DispatchServiceException extends RuntimeException`. Se unificó a `catch (RuntimeException)`.
- **`ProblemDetail.toJson()` no existe**: En Spring 6, `ProblemDetail` no tiene `toJson()`. Se inyectó `ObjectMapper` y se usó `writeValueAsString()`.
- **Mockito matchers mezclados**: `when(mock.method(actor, any()))` falla con `InvalidUseOfMatchers`. Se cambió a `eq(actor)` para que todos los argumentos sean matchers.
- **Testcontainers no detecta Docker 29.x**: El Docker Java client de Testcontainers 1.20.4 no soporta Docker Engine 29.9.2. Los integration tests se marcaron `@Disabled` con instrucciones para el propietario.

## Qué practicar ejecutando

1. Levantar el sistema con `docker compose up --build` y seguir el `docs/demo-script.md`.
2. Abrir Swagger UI y probar cada endpoint.
3. Crear un ticket, ver cómo se asigna, revisar el historial.
4. Esperar 60s y ver el escalamiento en logs.
5. Revisar `/actuator/prometheus` para ver las métricas.
6. Intentar acceder a un ticket ajeno → 403.
7. Enviar un token expirado → 401.
