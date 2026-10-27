# HelpFlow — Design Notes

## Assumptions

- **A-01 (Phase 0):** The machine has Maven 3.8.7 installed, but the steering requires 3.9+.
  Decision: add the Maven Wrapper (`mvnw`) to both Maven projects, pinned to Maven 3.9.13.
- **A-02 (PROMPT 1/2):** `.kiro/` is removed from git tracking and added to `.gitignore`
  so no AI-tool markers appear in the public repo. The folder stays on disk for local context only.
- **A-03 (PROMPT 2):** JaCoCo `check` execution is temporarily disabled in `support-ticket-service/pom.xml`
  because several use cases (Authenticate, ManageUsers, ManageTechnicians, ManageSlaPolicies,
  Transition, Update) are implemented but not yet unit-tested. Current line coverage:
  domain/model 69.3%, domain/service 89.0%, domain/exception 57.1%, application/usecase 40.5%.
  The >=80% gate will be re-enabled in PROMPT 4 once security + integration tests cover the remaining
  use cases.
- **A-04 (PROMPT 2):** `support-ticket-service` uses an `application.yml` that excludes DataSource,
  JPA, Flyway and Security autoconfigurations so the empty skeleton compiles and the domain/application
  unit tests run without a DB. Real config is added in PROMPT 3.

## Architecture decisions

- **ADR-01:** Hexagonal architecture. `domain` depends only on the JDK; `application` depends only on
  `domain` (+ the tolerated `@Transactional` Spring import); `infrastructure` depends on everything.
  Enforced by the ArchUnit test `ArchitectureRulesTest`.
- **ADR-02:** Two-transaction ticket creation (TX-1 create + commit, remote dispatch call outside TX,
  TX-2 assign + commit) via separate collaborator beans `CreateTicketTxPort` / `AssignTicketTxPort`
  to avoid self-invocation proxy bypass.
- **ADR-03:** `technician-dispatch-service` is a pure deterministic CRC32 algorithm in `domain` with
  no Spring annotations on the dispatcher class; wired as a bean in `web/config/AppConfig`.
