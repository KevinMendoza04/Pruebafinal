# Fase 2 — Fundamentos, dominio y microservicio de despacho

## Entidades y enums del dominio

Modelos en `support-ticket-service/src/main/java/com/riwi/helpflow/ticket/domain/model/`:
- `User` (id, document, fullName, email, site, department, role, status, passwordHash)
- `Technician` (id, userId, code, specialties, site, supportProfile, status, maxActiveTickets)
- `Ticket` (id, requesterId, title, description, category, priority, site, affectedAsset, status, assignedTechnicianId, createdAt, slaDeadline, firstRespondedAt, resolvedAt, escalatedAt, slaBreachedAt)
- `TicketHistoryEntry` (id, ticketId, eventType, fromStatus, toStatus, actorId, observation, createdAt)
- `Assignment` (id, ticketId, technicianId, assignedAt, unassignedAt)
- `SlaPolicy` (id, priority, resolutionHours)

Enums:
- `TicketCategory`: HARDWARE, SOFTWARE, RED, ACCESOS, OTRO
- `TicketPriority`: BAJA, MEDIA, ALTA, CRITICA (con método `escalate()`)
- `TicketStatus`: ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO
- `Role`: ROLE_USUARIO, ROLE_TECNICO, ROLE_ADMIN
- `HistoryEventType`: CREATED, ASSIGNED, REASSIGNED, STATUS_CHANGED, ESCALATED, ASSIGNMENT_FAILED

## Diferencia entre entidad de dominio y entidad JPA

La entidad de dominio (`domain/model/Ticket.java`) es un objeto rico con comportamiento: `create()`, `applyTransition()`, `assignTo()`, `escalate()`, `isOverdue()`. No tiene anotaciones de JPA ni de Spring.

La entidad JPA (que se crea en la Fase 3) vive en `infrastructure/adapter/out/persistence/` y tiene `@Entity`, `@Table`, `@Column`, relaciones `@OneToMany`. Es un detalle de persistencia que el dominio nunca ve. Un mapper (MapStruct) convierte entre ambas.

## Puertos y adaptadores

**Puertos de salida** (`application/port/out/`): interfaces que el dominio/aplicación necesita del exterior:
- `UserRepositoryPort`, `TechnicianRepositoryPort`, `TicketRepositoryPort`, `AssignmentRepositoryPort`, `TicketHistoryRepositoryPort`, `SlaPolicyRepositoryPort`
- `TechnicianDispatchPort` (llama al servicio de despacho)
- `PasswordHasherPort`, `TokenIssuerPort` (seguridad)
- `TicketMetricsPort` (métricas — Micrometer nunca entra en el dominio)

**Puertos de entrada** (`application/port/in/`): interfaces de los casos de uso (RegisterUser, Authenticate, CreateTicket, GetTickets, UpdateTicket, TransitionTicket, CancelTicket, ReassignTicket, EscalateTickets, ManageSlaPolicies, ManageUsers, ManageTechnicians).

Los adaptadores (que se implementan en la Fase 3) concretan estas interfaces con JPA, RestClient, BCrypt, Micrometer.

## Cómo funciona el servicio de despacho

`technician-dispatch-service` expone `POST /technician-dispatch` con body `{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}`.

El algoritmo en `domain/TechnicianDispatcher`:
1. Construye la cadena `"125|RED|ALTA"` en UTF-8.
2. Calcula CRC32 → número entero sin signo (0..4294967295).
3. `techNum = (seed % 20) + 1` → código `TEC-%03d`.
4. `hours = (seed / 20) % 13` → 0..12.
5. Disponibilidad: 0-2 ALTA, 3-8 MEDIA, >8 BAJA.

Es determinista: la misma entrada siempre produce la misma salida.

## Por qué el despacho no necesita base de datos

El algoritmo es puramente matemático (CRC32). No consulta datos de técnicos; solo devuelve un código determinista. No hay estado que persistir ni usuarios que autenticar. Es un servicio auxiliar simulado.

## Cómo se comunican ambos servicios

`support-ticket-service` llama a `technician-dispatch-service` mediante un adaptador REST que implementa `TechnicianDispatchPort`. Usa `RestClient` con timeouts (2s conexión, 3s lectura, sin reintentos). Si el despacho falla, el ticket se queda en ABIERTO con un evento de historial `ASSIGNMENT_FAILED`; nunca se pierde.

## Resultados reales de pruebas

```
technician-dispatch-service: 28 tests, 0 failures — BUILD SUCCESS
support-ticket-service: 73 tests, 0 failures — BUILD SUCCESS
```

Cobertura actual de `support-ticket-service` (JaCoCo, sin gate temporalmente):
- domain/model 69.3%, domain/service 89.0%, domain/exception 57.1%, application/usecase 40.5%
- El gate >=80% se reactiva en la Fase 4 con tests de seguridad e integración.
