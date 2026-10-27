# HelpFlow — IT Support Ticket System

> **Status:** 🚧 Under construction — Phase 0 (specs & scaffolding)

## Overview

HelpFlow is a distributed IT support ticket management system built with two Spring Boot microservices:

- **support-ticket-service** — Core service: user/technician management, ticket lifecycle, JWT security, SLA tracking, metrics.
- **technician-dispatch-service** — Lightweight dispatch oracle: given a ticket category and priority, returns the suggested technician and estimated response time.

## Architecture

Pure hexagonal (ports & adapters) per service. No framework imports in the domain layer.

## Quick Start

> Full instructions will be added when the implementation is complete (Phase 5).

```bash
docker compose up --build
```

## Roles

| Role            | Permissions                                          |
|-----------------|------------------------------------------------------|
| ROLE_USUARIO    | Create and view own tickets                          |
| ROLE_TECNICO    | View and manage assigned tickets                     |
| ROLE_ADMIN      | Full access, reassignment, SLA configuration         |

## Documentation

- `docs/diagrams/` — Architecture, lifecycle, ER, microservices, and sequence diagrams
- `docs/evidence/` — Test screenshots, metrics evidence, SLA reports
- `docs/defense/` — Phase defense documents (Spanish)
- `postman/` — Postman collection
- `.kiro/specs/helpflow/` — Full requirements, design, and task specs

## License

Internal assessment — Riwi M6.1
