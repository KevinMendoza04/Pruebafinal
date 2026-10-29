# HelpFlow — IT Support Ticket System

A modular, secure and observable IT support ticket system built with **Java 21**, **Spring Boot 3.5.x**, and **hexagonal architecture**. Consists of two microservices communicating via REST.

## Architecture

```
┌──────────────────────────────┐     ┌─────────────────────────────┐
│  support-ticket-service       │     │  technician-dispatch-service  │
│  (port 8080)                   │────▶│  (port 8081)                  │
│  Hexagonal: domain/application │     │  Pure deterministic dispatch  │
│  /infrastructure separation     │     │  No DB, no security           │
│  PostgreSQL + Flyway           │     │                               │
│  JWT security + roles          │     │                               │
└──────────────────────────────┘     └─────────────────────────────┘
```

### Hexagonal layers (support-ticket-service)

- **domain/**: Rich models (`User`, `Ticket`, `Technician`, `Assignment`, `TicketHistoryEntry`, `SlaPolicy`), domain services (`TicketTransitionPolicy`, `SlaCalculator`, `TechnicianSelectionPolicy`, `EscalationPolicy`), business exceptions. No Spring/JPA/Jackson/Lombok.
- **application/**: Use case interfaces (ports in) and implementations, outbound ports (interfaces). Orchestrates domain logic. The only tolerated Spring import is `@Transactional`.
- **infrastructure/**: REST controllers, JPA entities, Spring Data repos, MapStruct mappers for DTOs (manual mapper for entities), RestClient dispatch adapter, Micrometer metrics adapter, JWT security, Flyway migrations.

### Ticket lifecycle

```
ABIERTO → ASIGNADO → EN_PROGRESO → RESUELTO → CERRADO
   |         |           |              ↕
   ↓         ↓           ↓          (reopen)
CANCELADO  CANCELADO   CANCELADO
```

## Prerequisites

- Java 21
- Maven 3.9+ (or use the included `mvnw` wrapper)
- Docker + Docker Compose

## Running with Docker Compose

```bash
# Clone and start everything
git clone https://github.com/KevinMendoza04/Pruebafinal
cd Pruebafinal
docker compose up --build

# Services will be available at:
#   Main service:    http://localhost:8080
#   Dispatch service: http://localhost:8081
#   PostgreSQL:      localhost:5432
```

## Running locally without Docker

```bash
# Start PostgreSQL (adjust as needed)
docker run -d --name helpflow-db -p 5432:5432 \
  -e POSTGRES_DB=helpflow -e POSTGRES_USER=helpflow -e POSTGRES_PASSWORD=helpflow \
  postgres:16-alpine

# Start dispatch service
cd technician-dispatch-service && ./mvnw spring-boot:run

# In another terminal, start main service
cd support-ticket-service && ./mvnw spring-boot:run
```

## Environment variables

See `.env.example` for all options. Key variables:

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/helpflow` | Database URL |
| `JWT_SECRET` | (placeholder) | JWT signing secret (min 32 bytes) |
| `JWT_EXPIRATION_MINUTES` | `60` | Token expiration |
| `DISPATCH_BASE_URL` | `http://localhost:8081` | Dispatch service URL |
| `ADMIN_DOCUMENT` | `ADMIN-000` | Admin document for bootstrap |
| `ADMIN_EMAIL` | `admin@helpflow.com` | Admin email for bootstrap |
| `ADMIN_PASSWORD` | (empty) | Admin password (required for bootstrap) |
| `CORPORATE_EMAIL_DOMAIN` | `helpflow.com` | Valid email domain for registration |
| `ESCALATION_SCAN_INTERVAL` | `60s` | Escalation scan frequency |
| `ESCALATION_THRESHOLD_PERCENT` | `80` | Escalation threshold |

## Endpoints

### Auth (public)
- `POST /auth/register` — Register new user (always ROLE_USUARIO)
- `POST /auth/login` — Login, returns JWT

### Users
- `GET /users/me` — Own profile (any role)
- `PUT /users/me` — Update own profile
- `GET /users` — List users (ADMIN)
- `GET /users/{id}` — Get user (ADMIN)
- `PUT /users/{id}` — Update user (ADMIN)
- `PATCH /users/{id}/status` — Activate/deactivate user (ADMIN)

### Technicians (ADMIN unless noted)
- `POST /technicians` — Create technician
- `GET /technicians` — List technicians
- `GET /technicians/{id}` — Get technician
- `PUT /technicians/{id}` — Update technician
- `PATCH /technicians/{id}/status` — Activate/deactivate technician
- `GET /technicians/me` — Own technician profile (TECNICO)

### Tickets
- `POST /tickets` — Create ticket (USUARIO/ADMIN)
- `GET /tickets` — List tickets (role-scoped, paged, filters)
- `GET /tickets/{id}` — Get ticket detail
- `PUT /tickets/{id}` — Edit ticket (owner/admin, ABIERTO only)
- `DELETE /tickets/{id}` — Cancel ticket (logical delete)
- `POST /tickets/{id}/transitions` — Transition ticket
- `GET /tickets/{id}/history` — Get ticket history

### Admin
- `PUT /admin/tickets/{id}/reassign` — Reassign ticket
- `GET /admin/sla-policies` — Get SLA policies
- `PUT /admin/sla-policies` — Update SLA policies
- `POST /admin/escalation/run` — Trigger escalation scan

### Dispatch service
- `POST /technician-dispatch` — Suggest technician (deterministic)

### Actuator
- `GET /actuator/health` — Health check (public)
- `GET /actuator/info` — App info (public)
- `GET /actuator/metrics` — Metrics (ADMIN)
- `GET /actuator/prometheus` — Prometheus endpoint (ADMIN)

## SLA policies

| Priority | Initial hours |
|---|---|
| CRITICA | 2 |
| ALTA | 4 |
| MEDIA | 8 |
| BAJA | 24 |

Cross-validation: CRITICA < ALTA < MEDIA < BAJA (strictly increasing). Changes apply only to new tickets.

## Assignment flow

1. TX-1: validate requester ACTIVO, create ticket ABIERTO + SLA + history CREATED → commit
2. Outside TX: call dispatch service (REST, 2s/3s timeouts, no retries)
3. TX-2: evaluate suggestion (specialty, capacity, site, SLA ETA) or fallback → assign → commit
4. No eligible: ticket stays ABIERTO, history ASSIGNMENT_FAILED, HTTP 201 returned

## Testing

```bash
# Unit + integration tests
cd support-ticket-service && ./mvnw test
cd technician-dispatch-service && ./mvnw test

# Coverage report
./mvnw verify  # JaCoCo report at target/site/jacoco/index.html
```

## Swagger / OpenAPI

- Main service: http://localhost:8080/swagger-ui.html
- Dispatch service: http://localhost:8081/swagger-ui.html

## Entity Relationship Diagram

```mermaid
%% ER Diagram — HelpFlow
erDiagram
    users ||--o{ tickets : "requester_id"
    users ||--|| technicians : "1:1 profile"
    technicians ||--o{ tickets : "assigned_technician_id"
    tickets ||--o{ ticket_history : "1:N history"
    tickets ||--o{ assignments : "1:N assignments"
    sla_policies ||--o{ tickets : "priority → SLA deadline"

    users {
        uuid id PK
        string document UK
        string email UK
        string full_name
        string site
        string department
        enum role
        enum status
        string password_hash
    }

    technicians {
        uuid id PK
        uuid user_id FK UK
        string code UK
        set specialties
        string site
        string support_profile
        enum status
        int max_active_tickets
    }

    tickets {
        uuid id PK
        uuid requester_id FK
        string title
        string description
        enum category
        enum priority
        string site
        string affected_asset
        enum status
        uuid assigned_technician_id FK
        timestamptz created_at
        timestamptz sla_deadline
        timestamptz escalated_at
        bigint version
    }

    ticket_history {
        uuid id PK
        uuid ticket_id FK
        enum event_type
        enum from_status
        enum to_status
        uuid actor_id
        string observation
        timestamptz created_at
    }

    assignments {
        uuid id PK
        uuid ticket_id FK
        uuid technician_id FK
        timestamptz assigned_at
        timestamptz unassigned_at
    }

    sla_policies {
        uuid id PK
        enum priority UK
        int resolution_hours
    }
```

## Repository structure

```
Pruebafinal/
├── support-ticket-service/       # Main service
├── technician-dispatch-service/  # Dispatch service
├── docker-compose.yml
├── .env.example
├── postman/                      # Postman collection
├── docs/
│   ├── defense/                  # Spanish study material (fase1-5, guia-sustentacion)
│   ├── diagrams/                 # Mermaid diagrams
│   ├── design-notes.md            # Assumptions + architecture decisions
│   └── assessment-checklist.md    # Traceability matrix
```
