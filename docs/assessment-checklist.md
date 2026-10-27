# HelpFlow — Assessment Checklist

> Status values: `NOT_STARTED` | `IN_PROGRESS` | `IMPLEMENTED` | `VERIFIED` | `BLOCKED`
> Rubric weights: Architecture 20 · Functionality 20 · Security 20 · Quality 20 · Defense 10 · Docs 10

## Part 1 — Domain model, users and technicians (Architecture 20 pts)

| ID | Requirement | Where it will live | Verification | Status |
|---|---|---|---|---|
| ARCH-01 | Hexagonal structure: pure domain, ports, adapters | `domain/`, `application/port/`, `infrastructure/adapter/` | ArchUnit test | NOT_STARTED |
| ARCH-02 | Domain has NO Spring/JPA/Jackson/Lombok deps | `domain/model`, `domain/service`, `domain/exception` | ArchUnit test | NOT_STARTED |
| ARCH-03 | Application layer depends only on domain | `application/usecase`, `application/port/in`, `application/port/out` | ArchUnit test | NOT_STARTED |
| ARCH-04 | Two independent Maven projects | `support-ticket-service/pom.xml`, `technician-dispatch-service/pom.xml` | `mvn verify` on each | NOT_STARTED |
| ARCH-05 | User, Technician, Ticket, TicketHistoryEntry, Assignment, SlaPolicy models | `domain/model/` | Unit tests | NOT_STARTED |
| ARCH-06 | TicketTransitionPolicy, SlaCalculator, TechnicianSelectionPolicy, EscalationPolicy | `domain/service/` | Unit tests | NOT_STARTED |
| ARCH-07 | All 10 outbound ports defined | `application/port/out/` | ArchUnit + compile | NOT_STARTED |
| ARCH-08 | MapStruct mappers (DTO↔command, entity↔domain) | `infrastructure/adapter/in/rest`, `infrastructure/adapter/out/persistence` | compile + mapper generated | NOT_STARTED |
| ARCH-09 | Diagrams: hexagonal, ticket lifecycle, microservices, ER, assignment sequence | `docs/diagrams/` | visual review | NOT_STARTED |
| ARCH-10 | `@Transactional` on separate bean (no self-invocation); TX-1/TX-2 split for create+assign | `application/usecase/` | integration test | NOT_STARTED |

## Part 2 — Tickets, persistence, SLA, assignment, escalation (Functionality 20 pts)

| ID | Requirement | Where it will live | Verification | Status |
|---|---|---|---|---|
| FR-01 | Create ticket (ABIERTO, SLA deadline set) — USUARIO/ADMIN only for self | `application/usecase/CreateTicketUseCase` | integration test | NOT_STARTED |
| FR-02 | Ticket fields: title(5-120), description(10-2000), category, priority, site, affectedAsset(2-100) | `infrastructure/adapter/in/rest` DTOs | Bean Validation test | NOT_STARTED |
| FR-03 | SLA deadline = createdAt + hours(priority) from `sla_policies` | `domain/service/SlaCalculator` | unit test | NOT_STARTED |
| FR-04 | Assignment flow: TX-1 create → dispatch call → TX-2 assign | `application/usecase/AssignTicketUseCase` | integration test | NOT_STARTED |
| FR-05 | Dispatch fallback: eligibility rules (specialty, site, load, SLA ETA) | `domain/service/TechnicianSelectionPolicy` | unit test | NOT_STARTED |
| FR-06 | No-eligible path: ticket stays ABIERTO, history ASSIGNMENT_FAILED, HTTP 201 | `application/usecase/AssignTicketUseCase` | integration test | NOT_STARTED |
| FR-07 | Ticket state transitions per matrix (TicketTransitionPolicy) | `domain/service/TicketTransitionPolicy` | unit test | NOT_STARTED |
| FR-08 | Ticket history entry on every transition | `domain/model/TicketHistoryEntry` | integration test | NOT_STARTED |
| FR-09 | Escalation scheduler (threshold %) — raise priority, set escalatedAt, retry assign | `application/usecase/EscalateTicketsUseCase` + scheduler | unit + integration test | NOT_STARTED |
| FR-10 | Escalation is idempotent (once per ticket) | `domain/service/EscalationPolicy` | unit test | NOT_STARTED |
| FR-11 | slaBreachedAt set first time ticket found overdue | `application/usecase/EscalateTicketsUseCase` | integration test | NOT_STARTED |
| FR-12 | Admin reassignment (PUT /admin/tickets/{id}/reassign) | `application/usecase/ReassignTicketUseCase` | integration test | NOT_STARTED |
| FR-13 | Logical delete (CANCELADO), never physical | `application/usecase/CancelTicketUseCase` | integration test | NOT_STARTED |
| FR-14 | Edit ticket (title, description, affectedAsset) only while ABIERTO | `application/usecase/UpdateTicketUseCase` | integration test | NOT_STARTED |
| FR-15 | Avoid N+1: EntityGraph/join fetch on list/detail queries | `infrastructure/adapter/out/persistence` | Hibernate-statistics test | NOT_STARTED |
| FR-16 | Optimistic locking on tickets; pessimistic lock on technician during assign | `infrastructure/adapter/out/persistence` entities | concurrency test | NOT_STARTED |
| FR-17 | Flyway V1/V2/V3; ddl-auto=validate | `src/main/resources/db/migration/` | Testcontainers boot test | NOT_STARTED |
| FR-18 | Admin SLA policy update (PUT /admin/sla-policies) with cross-validation | `application/usecase/UpdateSlaPoliciesUseCase` | integration test | NOT_STARTED |
| FR-19 | Demo seeder (profile `demo`, TEC-001..TEC-020) | `infrastructure/config/` | `demo` profile boot test | NOT_STARTED |
| FR-20 | Admin bootstrap from env vars (ADMIN_DOCUMENT, ADMIN_EMAIL, ADMIN_PASSWORD) | `infrastructure/config/` | integration test | NOT_STARTED |

## Part 3 — Security, validation, global error handling (Security 20 pts)

| ID | Requirement | Where it will live | Verification | Status |
|---|---|---|---|---|
| SEC-01 | JWT HS256, claims: sub/email/role/iat/exp/jti; secret from env min 32 bytes | `infrastructure/security/` | unit test | NOT_STARTED |
| SEC-02 | Auth filter validates signature + expiry + user status ACTIVO | `infrastructure/security/JwtAuthenticationFilter` | integration test | NOT_STARTED |
| SEC-03 | POST /auth/register (always ROLE_USUARIO, ACTIVO) | `application/usecase/RegisterUserUseCase` | integration test | NOT_STARTED |
| SEC-04 | POST /auth/login returns accessToken, tokenType, expiresIn | `application/usecase/LoginUseCase` | integration test | NOT_STARTED |
| SEC-05 | Role matrix enforced: USUARIO own tickets, TECNICO assigned tickets, ADMIN all | use case ownership checks | integration test (role matrix) | NOT_STARTED |
| SEC-06 | IDOR denied → 403 (not 404); no token → 401 | use case + security filter | integration test | NOT_STARTED |
| SEC-07 | Custom AuthenticationEntryPoint (401) and AccessDeniedHandler (403) with ProblemDetail + traceId | `infrastructure/security/` | integration test | NOT_STARTED |
| SEC-08 | Stateless, CSRF disabled, BCrypt PasswordEncoder | `infrastructure/security/SecurityConfig` | integration test | NOT_STARTED |
| SEC-09 | TraceId filter: read X-Trace-Id or generate UUID; MDC; response header | `infrastructure/web/TraceIdFilter` | integration test | NOT_STARTED |
| SEC-10 | ProblemDetail (RFC 7807) with type/title/status/detail/instance/timestamp/traceId | `infrastructure/adapter/in/rest/ApiExceptionHandler` | integration test | NOT_STARTED |
| SEC-11 | Covers: 400/401/403/404/409/422/500/502/503 in error handler | `ApiExceptionHandler` | integration test per error | NOT_STARTED |
| SEC-12 | Bean Validation on all request DTOs | `infrastructure/adapter/in/rest` DTOs | validation test | NOT_STARTED |
| SEC-13 | Corporate email domain rule (helpflow.corporate-email-domain) | `domain/model/User` or use case | unit test | NOT_STARTED |
| SEC-14 | Actuator: health/info public; metrics/prometheus ROLE_ADMIN; no env/beans etc. | `infrastructure/config/` + `application.yml` | integration test | NOT_STARTED |
| ERR-01 | Structured logging (ECS format); no passwords/tokens in logs | `application.yml`, use cases | log output review | NOT_STARTED |
| ERR-02 | Auth failure counter `helpflow.auth.failures{reason}` | `infrastructure/security/` + `infrastructure/adapter/out/metrics` | metrics endpoint test | NOT_STARTED |
| ERR-03 | Access denied counter `helpflow.access.denied` | `infrastructure/security/` + metrics adapter | metrics endpoint test | NOT_STARTED |

## Part 4 — Microservices, dispatch, observability (Functionality / Architecture)

| ID | Requirement | Where it will live | Verification | Status |
|---|---|---|---|---|
| INT-01 | technician-dispatch-service: POST /technician-dispatch, no JPA, no security | `technician-dispatch-service/` | integration test | VERIFIED |
| INT-02 | Dispatch deterministic algorithm (CRC32 seed) | `technician-dispatch-service/domain/TechnicianDispatcher` | unit test | VERIFIED |
| INT-03 | Dispatch 400 on invalid enum/missing field | `technician-dispatch-service/web/handler/GlobalExceptionHandler` | unit test | VERIFIED |
| INT-04 | RestClient adapter with 2s connect / 3s read timeout, no retries | `infrastructure/adapter/out/dispatch/` | integration test (mock server) | NOT_STARTED |
| OBS-01 | `helpflow.tickets.created{category,priority}` counter | `infrastructure/adapter/out/metrics/` | metrics endpoint | NOT_STARTED |
| OBS-02 | `helpflow.tickets.first_response` timer | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-03 | `helpflow.tickets.resolution` timer | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-04 | `helpflow.tickets.overdue` GAUGE (count query) | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-05 | `helpflow.sla.breaches` counter | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-06 | `helpflow.tickets.escalated` counter | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-07 | `helpflow.dispatch.failures` counter | metrics adapter | metrics endpoint | NOT_STARTED |
| OBS-08 | TicketMetricsPort outbound port (Micrometer never leaks into use cases) | `application/port/out/TicketMetricsPort` | ArchUnit test | NOT_STARTED |
| OBS-09 | Swagger/springdoc on both services; bearer JWT documented | `infrastructure/config/` + springdoc dep | browser review | IMPLEMENTED |

## Part 5 — Testing, Docker, delivery (Quality 20 pts / Docs 10 pts)

| ID | Requirement | Where it will live | Verification | Status |
|---|---|---|---|---|
| TEST-01 | Unit tests for domain/application (no Spring context, no DB) | `src/test/java/.../domain`, `.../application` | `mvn test` | NOT_STARTED |
| TEST-02 | Integration tests: SpringBootTest + MockMvc + Testcontainers PostgreSQL | `src/test/java/.../integration` | `mvn verify` | NOT_STARTED |
| TEST-03 | ArchUnit: domain/application dependency rules + controller-repo prohibition | `src/test/java/.../arch` | `mvn test` | NOT_STARTED |
| TEST-04 | JaCoCo ≥ 80% line coverage on domain + application | JaCoCo report | `mvn verify` | NOT_STARTED |
| TEST-05 | Scenario: login → create → assign → transitions → history | integration test | `mvn verify` | NOT_STARTED |
| TEST-06 | Scenario: expired/invalid token → 401 | integration test | `mvn verify` | NOT_STARTED |
| TEST-07 | Scenario: role matrix + IDOR denial | integration test | `mvn verify` | NOT_STARTED |
| TEST-08 | Scenario: validation errors + ProblemDetail shape | integration test | `mvn verify` | NOT_STARTED |
| TEST-09 | Scenario: concurrency (capacity under concurrency) | integration test | `mvn verify` | NOT_STARTED |
| TEST-10 | Scenario: Flyway on clean DB | Testcontainers boot test | `mvn verify` | NOT_STARTED |
| TEST-11 | Scenario: N+1 query count proof | Hibernate-statistics test | `mvn verify` | NOT_STARTED |
| DEP-01 | Multi-stage Dockerfile for each service | `support-ticket-service/Dockerfile`, `technician-dispatch-service/Dockerfile` | `docker build` | NOT_STARTED |
| DEP-02 | docker-compose with both services + PostgreSQL | `docker-compose.yml` | `docker compose up` | NOT_STARTED |
| DEP-03 | `.env.example` with placeholder values (no secrets) | `.env.example` | review | NOT_STARTED || DOC-01 | README: description, endpoints, run local + docker, roles, assignment flow, transitions, SLA | `README.md` | review | NOT_STARTED |
| DOC-02 | Postman collection + environment (no secrets) | `postman/` | import + run | NOT_STARTED |
| DOC-03 | 5 Mermaid diagrams | `docs/diagrams/` | visual review | NOT_STARTED |
| DOC-04 | Evidence: metrics, overdue tickets, logs | `docs/evidence/` | review | NOT_STARTED |
| DOC-05 | Defense notes (phase files, Spanish) | `docs/defense/` | review | NOT_STARTED |
