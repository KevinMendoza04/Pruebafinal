---
inclusion: always
---
# HelpFlow — Repository Layout and Hexagonal Rules

## Repository (https://github.com/KevinMendoza04/Pruebafinal)
Two INDEPENDENT Maven projects (no shared parent pom), one folder each, built separately by Docker.

```
Pruebafinal/
├── .kiro/steering/*.md            (these files)
├── docs/design-notes.md           short log: Assumptions + Architecture decisions
├── docs/
│   ├── assessment/                official statement, guide, rubric
│   ├── diagrams/                  hexagonal-architecture.mmd, ticket-lifecycle.mmd,
│   │                              microservices-communication.mmd, er-diagram.mmd, assignment-sequence.mmd
│   ├── evidence/                  real screenshots, logs, metrics outputs
│   ├── defense/                   Spanish study material (phase-N notes, study guide)
│   ├── assessment-checklist.md  demo-script.md  troubleshooting.md
├── postman/                       collection + environment (no secrets)
├── scripts/                       demo.sh, make-zip.sh
├── support-ticket-service/        pom.xml, Dockerfile, .dockerignore, src/
├── technician-dispatch-service/   pom.xml, Dockerfile, .dockerignore, src/
├── docker-compose.yml  .env.example  .gitignore  README.md
```
`.gitignore` must exclude `.env`, `target/`, `dist/`, IDE folders. Never commit secrets.

## support-ticket-service packages (base `com.riwi.helpflow.ticket`)
```
domain/model          User, Technician, Ticket, TicketHistoryEntry, Assignment, SlaPolicy, enums
domain/service        TicketTransitionPolicy, SlaCalculator, TechnicianSelectionPolicy, EscalationPolicy
domain/exception      business exceptions (no HTTP, no Spring)
application/port/in   use-case interfaces (one per use case)
application/port/out  UserRepositoryPort, TechnicianRepositoryPort, TicketRepositoryPort,
                      AssignmentRepositoryPort, SlaPolicyRepositoryPort, TechnicianDispatchPort,
                      PasswordHasherPort, TokenIssuerPort, TicketMetricsPort
application/usecase   use-case implementations (orchestration only)
infrastructure/adapter/in/rest        controllers, request/response DTOs, MapStruct mappers, ApiExceptionHandler
infrastructure/adapter/out/persistence JPA entities, Spring Data repositories, adapters, MapStruct mappers
infrastructure/adapter/out/dispatch   RestClient adapter implementing TechnicianDispatchPort
infrastructure/adapter/out/metrics    Micrometer adapter implementing TicketMetricsPort
infrastructure/security               JWT filter, SecurityConfig, entry point, access-denied handler, BCrypt adapter
infrastructure/config                 beans, properties, Clock, bootstrap/demo seeders, scheduler, OpenAPI
infrastructure/web                    TraceIdFilter
```
technician-dispatch-service: small hexagonal-style layout is enough (`domain` for seed/availability logic, `web` for controller/DTOs/handler).

## Dependency rules (enforced by ArchUnit)
- `domain` -> only the JDK (`java.time`, `java.util`). No Spring, no Jakarta, no Jackson, no Lombok, no MapStruct.
- `application` -> `domain` only (+ the transaction annotation `org.springframework.transaction.annotation.Transactional` is the ONE tolerated Spring import, because the statement requires `@Transactional` on assign/escalate; document the trade-off in an ADR-style note in `design.md`).
- `infrastructure` -> may depend on everything; nothing depends on it.
- A `@Transactional` boundary must be on a different Spring bean than its caller (avoid self-invocation). The create-ticket orchestration is NOT transactional as a whole; it calls two transactional collaborators (TX-1 create, TX-2 assign) around the remote dispatch call.
- Use cases receive the authenticated actor as an explicit argument (`AuthenticatedActor`: userId, role) built in the REST adapter from the security context. The domain never reads the SecurityContext.
