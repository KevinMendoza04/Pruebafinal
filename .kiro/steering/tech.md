---
inclusion: always
---
# HelpFlow — Technical Stack and Rules

## Versions (pin them explicitly; verify they resolve on Maven Central)
- Java 21 (the owner has Java 21 installed). Maven 3.9+. 
- Spring Boot **3.5.x** (latest stable 3.5 patch). Do NOT use Spring Boot 4.
- springdoc-openapi-starter-webmvc-ui 2.8.x, MapStruct 1.6.x (+ mapstruct-processor), jjwt 0.12.x (api/impl/jackson), Flyway (managed by Boot) + `flyway-database-postgresql`, PostgreSQL driver, `micrometer-registry-prometheus`, JaCoCo, ArchUnit (test), Testcontainers via `spring-boot-testcontainers` + `org.testcontainers:postgresql` + `junit-jupiter`, PostgreSQL image `postgres:16-alpine`.
- Lombok: allowed ONLY in infrastructure classes (JPA entities, DTOs). NEVER in `domain`. Prefer Java records for DTOs and commands.
- If a version combination does not compile or start, fix it with the smallest change and record it in `design.md`.

## Language rules (from the official guide)
- ALL code, identifiers, packages, class/method names, README, docs/diagrams, commit messages: English.
- Only user-facing messages (error `detail` texts, validation messages) may be Spanish.
- Exception: files under `docs/defense/` are personal study material written in Spanish for the owner.
- Enum values and the dispatch JSON field names remain Spanish as defined in `business-rules.md`.

## Mandatory technical rules
- Constructor injection only. No field `@Autowired`.
- No JPA entity is ever returned by a controller or used in the domain. Controllers never touch repositories.
- DTO <-> command/domain mapping and entity <-> domain mapping use MapStruct (`componentModel = "spring"`). Where MapStruct cannot reasonably map a rich domain object, write a small explicit mapper and justify it in a comment. The build must compile the generated mappers.
- Flyway migrations (never edit an applied one): `V1__schema.sql` (users, technicians, technician_specialties, tickets, constraints, indexes), `V2__asignaciones_historial_sla.sql` (assignments, ticket_history, sla_policies, indexes), `V3__datos_iniciales.sql` (default SLA policies only). Hibernate `ddl-auto=validate`.
- Unique constraints: users.document, users.email, technicians.code, technicians.user_id. Index: tickets(status), tickets(sla_deadline), tickets(requester_id), tickets(assigned_technician_id), ticket_history(ticket_id).
- Avoid N+1: use `@EntityGraph`/`join fetch` on the list/detail queries that need relations; LAZY by default for all associations; justify each fetch strategy in a comment. Prove it with a Hibernate-statistics test.
- Optimistic locking (`@Version`) on tickets; pessimistic lock on the technician row during assignment.
- JWT: HS256, `JWT_SECRET` (min 32 bytes) from env, `JWT_EXPIRATION_MINUTES` (default 60). Claims: `sub` (user id), `email`, `role`, `iat`, `exp`, `jti`. The auth filter validates signature/expiry AND loads the user to verify status `ACTIVO`. Stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled (stateless API), BCrypt `PasswordEncoder`.
- Security errors: custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) returning ProblemDetail JSON with `traceId`. Counters: `helpflow.auth.failures`, `helpflow.access.denied`.
- Coarse role checks with `@PreAuthorize` (enable method security); ownership/assignment checks inside the use cases.
- Errors: `@RestControllerAdvice` + `ProblemDetail` (RFC 7807). Every error body has `type`, `title`, `status`, `detail`, `instance`, `timestamp`, `traceId`. `type` = `https://helpflow.local/problems/<slug>`. Cover: validation (400), invalid transition (409/422 — pick one and document), not found (404), duplicate document/email (409), data integrity (409), enum/body parse errors (400), unauthenticated (401), forbidden (403), dispatch failure (502/503 only where surfaced), unexpected (500, no stack trace/SQL leaked).
- Trace id: a servlet filter with highest precedence reads `X-Trace-Id` if it matches `^[A-Za-z0-9-]{8,64}$`, else generates a UUID; puts it in MDC key `traceId`, returns it in the `X-Trace-Id` response header; used in logs and in every ProblemDetail (including 401/403 produced by Spring Security).
- Structured logging: Spring Boot native `logging.structured.format.console=ecs`. Log events with key facts (ticket id, status, technician code, actor id) but NEVER passwords, tokens, secrets or unnecessary personal data.
- Actuator (both services): expose `health,info,metrics,prometheus`. In the main service `health` and `info` are public; `metrics` and `prometheus` require `ROLE_ADMIN`. Do not expose `env`, `configprops`, `beans`, etc.
- Metrics (Micrometer, low-cardinality tags only — never ids/emails/tokens):
  - `helpflow.tickets.created{category,priority}` counter
  - `helpflow.tickets.first_response` timer; `helpflow.tickets.resolution` timer (real elapsed durations from stored timestamps)
  - `helpflow.tickets.overdue` GAUGE = current overdue tickets (count query), NOT a counter
  - `helpflow.sla.breaches` counter = cumulative breach events
  - `helpflow.tickets.escalated` counter
  - `helpflow.auth.failures{reason}` and `helpflow.access.denied` counters
  - `helpflow.dispatch.failures` counter
  The application layer reports events through an outbound port (`TicketMetricsPort`) implemented in infrastructure with Micrometer, so Micrometer never leaks into the use cases.
- Swagger: springdoc on both services; main service documents bearer JWT, request/response schemas, error examples, enum values.
- Config via environment variables with safe defaults for local dev only. `.env` is git-ignored; `.env.example` has placeholders only.

## Environment variables
Main service: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `DISPATCH_BASE_URL`, `ADMIN_DOCUMENT`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `CORPORATE_EMAIL_DOMAIN`, `ESCALATION_SCAN_INTERVAL`, `ESCALATION_THRESHOLD_PERCENT`, `DEMO_SEED_PASSWORD`, `SPRING_PROFILES_ACTIVE`.
Ports: main service 8080, dispatch service 8081.

## Testing rules
- Unit tests: JUnit 5 + Mockito (+ AssertJ). Domain/application tests run with NO Spring context and NO database. `TechnicianDispatchPort` is mocked.
- Integration tests: `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL (singleton container, `@ServiceConnection`). The dispatch port is replaced with `@MockitoBean`. Clean data between tests. No H2.
- ArchUnit test enforces: `domain..` has no dependency on Spring, Jakarta Persistence, Jackson, or `infrastructure..`/`application..`; `application..` does not depend on `infrastructure..`; controllers do not use repositories.
- JaCoCo report; enforce >= 80% line coverage on `domain` and `application` packages; report the real overall number. Never fake coverage.
- Required scenarios include: end-to-end login -> create ticket -> auto-assignment -> transitions -> history; expired/invalid token; role matrix; ownership (IDOR) denial; validation errors and error body shape; capacity under concurrency; Flyway on a clean DB; N+1 query count.
