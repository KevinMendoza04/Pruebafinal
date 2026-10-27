---
inclusion: always
---
# HelpFlow — Closed Business Decisions (do not change without asking the owner)

## Enums (Java identifiers in English, VALUES exactly as below)
- `UserStatus` / `TechnicianStatus`: `ACTIVO`, `INACTIVO`
- `TicketCategory`: `HARDWARE`, `SOFTWARE`, `RED`, `ACCESOS`, `OTRO`
- `TicketPriority`: `BAJA`, `MEDIA`, `ALTA`, `CRITICA` (no accent, everywhere: Java, JSON, DB, docs)
- `TicketStatus`: `ABIERTO`, `ASIGNADO`, `EN_PROGRESO`, `RESUELTO`, `CERRADO`, `CANCELADO`
- `Role`: `ROLE_USUARIO`, `ROLE_TECNICO`, `ROLE_ADMIN`
- `Availability` (dispatch): `ALTA`, `MEDIA`, `BAJA`

## Identity model
- `users` table holds credentials + role + profile (document, full name, corporate email, site, department, status).
- Technician = a profile (1:1) linked to a `users` row with role `ROLE_TECNICO`. Technician fields: `code` (unique, format `TEC-001`..`TEC-020`), specialties (set of `TicketCategory`), site, support profile (free text), status, `maxActiveTickets` (default 5).
- `POST /auth/register` ALWAYS creates `ROLE_USUARIO` with status `ACTIVO`. The request can never choose a role.
- Admin bootstrap: on startup, if no admin exists, create one from env vars `ADMIN_DOCUMENT`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`. Fail fast (outside tests) if `ADMIN_PASSWORD` is missing. Never hardcode secrets.
- Technicians are created by ADMIN (`POST /technicians`) which creates the user (role `ROLE_TECNICO`) + profile atomically.
- Demo data: a Spring profile `demo` seeds 20 technicians `TEC-001..TEC-020` (login emails `tec001@helpflow.com`...; password from env `DEMO_SEED_PASSWORD`), spread over a few sites and specialties. Flyway `V3` does NOT contain users or passwords.
- Corporate email rule: valid email format AND domain equals property `helpflow.corporate-email-domain` (default `helpflow.com`). Document it.
- Users are never hard-deleted (audit). Deactivate via status.

## Ticket rules
- Created by `ROLE_USUARIO` (or `ROLE_ADMIN`) for the AUTHENTICATED identity only. Requester ACTIVO is mandatory.
- Client-supplied fields: title (5–120), description (10–2000), category, priority, site, affectedAsset (2–100). Everything else (requester, status, SLA deadline, timestamps, technician) is server-controlled; DTOs must not contain those fields.
- Initial status `ABIERTO`. `DELETE /tickets/{id}` = LOGICAL cancel (transition to `CANCELADO`, history kept), never a physical delete.
- Ticket editing (`PUT /tickets/{id}`: title, description, affectedAsset) only while `ABIERTO`, by owner or admin.

## SLA
- Single SLA = resolution deadline: `slaDeadline = createdAt + hours(priority)`.
- Defaults (table `sla_policies`, seeded in `V3__datos_iniciales`): `CRITICA` 2h, `ALTA` 4h, `MEDIA` 8h, `BAJA` 24h. Admin can change via `PUT /admin/sla-policies`. Cross-validation: hours must be strictly increasing CRITICA < ALTA < MEDIA < BAJA. Changes apply only to NEW tickets.
- Use an injectable `java.time.Clock` everywhere (never `Instant.now()` directly in business code).
- First response = time from `createdAt` to the first move to `EN_PROGRESO` (`firstRespondedAt`). Resolution time = `createdAt` to `RESUELTO` (`resolvedAt`).
- "Overdue" = `slaDeadline < now` AND status in (`ABIERTO`,`ASIGNADO`,`EN_PROGRESO`).

## Escalation
- Scheduler runs every `helpflow.escalation.scan-interval` (default 60s) and calls a use case with a `Clock`; the use case is testable without waiting.
- Eligible: status in (`ABIERTO`,`ASIGNADO`,`EN_PROGRESO`), not yet escalated (`escalatedAt` is null), and elapsed time >= `helpflow.escalation.threshold-percent` (default 80%) of the SLA window.
- Action (inside ONE transaction): raise priority one level (`BAJA→MEDIA→ALTA→CRITICA`; `CRITICA` stays), keep the original `slaDeadline`, set `escalatedAt`, add history event `ESCALATED`, log, count metric. If the ticket is still `ABIERTO` (unassigned), retry assignment.
- Idempotent: a ticket is escalated at most once.
- Also set `slaBreachedAt` the first time a ticket is found overdue (counter metric for cumulative breaches).
- Demo endpoint: `POST /admin/escalation/run` (ADMIN) triggers the same use case on demand.

## Transition matrix (the ONLY legal transitions)
| From | To | Who | Observation |
|---|---|---|---|
| ABIERTO | ASIGNADO | system (assignment) / ADMIN | auto |
| ABIERTO | CANCELADO | owner USUARIO, ADMIN | required |
| ASIGNADO | EN_PROGRESO | assigned TECNICO, ADMIN | optional |
| ASIGNADO | CANCELADO | owner USUARIO, ADMIN | required |
| EN_PROGRESO | RESUELTO | assigned TECNICO, ADMIN | required |
| EN_PROGRESO | CANCELADO | ADMIN | required |
| RESUELTO | CERRADO | owner USUARIO, ADMIN | optional |
| RESUELTO | EN_PROGRESO (reopen) | assigned TECNICO, owner USUARIO, ADMIN | required |
`CERRADO` and `CANCELADO` are terminal. Implement as a dedicated, unit-tested domain policy (`TicketTransitionPolicy`).
Every transition writes a history entry: ticketId, eventType, fromStatus, toStatus, actor (user id or `SYSTEM`), observation, createdAt. Event types: `CREATED`, `ASSIGNED`, `REASSIGNED`, `STATUS_CHANGED`, `ESCALATED`, `ASSIGNMENT_FAILED`.

## Assignment flow
1. TX-1: validate, check requester ACTIVO, create ticket `ABIERTO` + SLA deadline + history `CREATED`; commit.
2. OUTSIDE any transaction: call `TechnicianDispatchPort.suggest(ticketId, category, priority)` (REST adapter, connect timeout 2s, read timeout 3s, no retries).
3. TX-2: re-load ticket; evaluate:
   - Hard eligibility: technician ACTIVO, category-compatible (technician specialties contain the category, or category is `OTRO`), active tickets (`ASIGNADO`+`EN_PROGRESO`) < `maxActiveTickets`.
   - Site rule: for `ALTA`/`CRITICA` the technician site must equal the ticket site (hard). For `BAJA`/`MEDIA` same site is preferred (soft).
   - SLA rule: the dispatch `tiempoEstimadoHoras` must be <= hours remaining to `slaDeadline`.
   - Recommended technician is accepted ONLY if every rule passes. Otherwise FALLBACK: among eligible technicians of the main service, order by (same site first, fewest active tickets, `code` ascending) and take the first. Fallback candidates have no ETA; the SLA rule is skipped for them (documented).
   - Lock the chosen technician row (`PESSIMISTIC_WRITE`) and re-count active tickets before assigning to avoid exceeding capacity under concurrency.
   - Success: create `Assignment`, ticket -> `ASIGNADO`, history `ASSIGNED`. All in the same TX.
4. No eligible technician OR dispatch failure/timeout: the ticket stays `ABIERTO`, history `ASSIGNMENT_FAILED` with reason, metric + log, HTTP 201 still returned with `assignedTechnician: null`. Never lose the ticket, never report a false assignment.
5. Admin reassignment (`PUT /admin/tickets/{id}/reassign`): allowed for `ABIERTO`/`ASIGNADO`/`EN_PROGRESO`; new technician must pass the hard eligibility rules; previous `Assignment` gets `unassignedAt`; new one created; history `REASSIGNED`.
- `TEC-xxx` codes returned by dispatch map to `technicians.code`.

## Dispatch service algorithm (deterministic)
- seed = CRC32 of the UTF-8 string `"<ticketId>|<categoria>|<prioridad>"` (explicit, documented; do NOT use `Random` or object hash codes).
- technician number = `(seed % 20) + 1` -> code `TEC-%03d`.
- estimated hours = `(seed / 20) % 13` (0..12).
- availability: 0–2 `ALTA`, 3–8 `MEDIA`, >8 `BAJA`.
- Contract (Spanish field names, exact): request `{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}`; response `{"ticketId":125,"tecnicoId":"TEC-014","disponibilidad":"MEDIA","tiempoEstimadoHoras":4}` (values above are only a format example). Invalid enum/missing field -> 400 ProblemDetail.

## Access control
- `ROLE_USUARIO`: create and read ONLY own tickets; edit/cancel/close own per matrix; read own profile.
- `ROLE_TECNICO`: read and transition ONLY tickets assigned to them (via their technician profile in the DB, never from request data).
- `ROLE_ADMIN`: everything, including users, technicians, reassignment, SLA config, escalation run, metrics endpoints.
- Accessing another person's ticket -> 403 (consistent, documented). No token/invalid token -> 401.
- Never trust ids in the body/params as proof of ownership; ownership is checked in the use case against persisted data and the authenticated principal.

## Endpoints (main service)
- Auth: `POST /auth/register`, `POST /auth/login` (returns `accessToken`, `tokenType`, `expiresIn`).
- Users: `GET /users/me`, `PUT /users/me`, `GET /users` (ADMIN, paged), `GET /users/{id}` (ADMIN), `PUT /users/{id}` (ADMIN), `PATCH /users/{id}/status` (ADMIN).
- Technicians (ADMIN unless noted): `POST /technicians`, `GET /technicians`, `GET /technicians/{id}`, `PUT /technicians/{id}`, `PATCH /technicians/{id}/status`, `GET /technicians/me` (TECNICO).
- Tickets: `POST /tickets`, `GET /tickets` (role-scoped, paged, filters status/priority/category), `GET /tickets/{id}`, `PUT /tickets/{id}`, `DELETE /tickets/{id}` (logical cancel), `POST /tickets/{id}/transitions` body `{targetStatus, observation}`, `GET /tickets/{id}/history`.
- Admin: `PUT /admin/tickets/{id}/reassign`, `GET /admin/sla-policies`, `PUT /admin/sla-policies`, `POST /admin/escalation/run`.
- Dispatch service: `POST /technician-dispatch`.
