---
inclusion: always
---
# HelpFlow — Product and Assessment Context

## What we are building
HelpFlow is an IT support ticket system made of TWO microservices:
1. `support-ticket-service` — main service (users, technicians, tickets, SLA, assignment, escalation, history, JWT security, metrics).
2. `technician-dispatch-service` — lightweight simulated external service (no JPA, no database, no security) that suggests a technician and an estimated attention time.

This is the final performance assessment (Module 6: Java + Spring Boot) at Riwi. The owner (Kem) must defend it orally, so every decision must be explainable and documented.

## Source of truth (read before every phase)
- `docs/assessment/enunciado_prueba_desempeno_m6_1-Java_SpringBoot.pdf` — official statement.
- `docs/assessment/Guia_de_la_prueba.txt` — official guide (language rules, deliverables, permissions).
- `docs/assessment/rubric.txt` — grading rubric (weights: Architecture 20, Functionality 20, Security 20, Quality 20, Defense 10, Documentation 10).

If a file is missing, STOP and tell the owner. Never invent its content.

## Known inconsistencies in the supplied material (already resolved — do not re-ask)
1. The rubric and two stray lines in the PDF mention affiliates, salary, affiliation date, credit applications and credit policies. They belong to a DIFFERENT assessment. IGNORE them completely. Never implement credit, loans or affiliates.
2. The rubric mentions H2 for test isolation; the guide requires Testcontainers. Use Testcontainers with PostgreSQL. Do not add H2.
3. The statement says dispatch answers "according to category, priority AND site", but the endpoint body only has `ticketId`, `categoria`, `prioridad`. DECISION: keep the body EXACTLY as the statement. Site is applied by the main service in its internal assignment rules.
4. The guide demands English code/README, but the statement's JSON contract and enum values are Spanish. DECISION: Java identifiers, packages, methods, README, commits in English; enum VALUES and the dispatch JSON field names stay exactly as the statement (Spanish). See `business-rules.md`.

## Hard rules about scope
- Do not invent business features beyond the statement. Technical pieces needed to satisfy a requirement are allowed.
- Anything not covered by a closed decision in these steering files: choose the simplest option that satisfies the statement, record it in `docs/design-notes.md` under "Assumptions", and mention it in the phase report. Ask the owner only if the choice is unsafe or irreversible.
