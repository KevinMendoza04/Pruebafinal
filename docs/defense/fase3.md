# Fase 3 — Persistencia, API REST y flujo completo de tickets

## Qué se construyó

La capa de persistencia completa (JPA + Flyway), los adaptadores de salida, los controladores REST con DTOs, el manejo global de errores ProblemDetail y los colaboradores transaccionales para el flujo de creación de tickets.

## Capas y flujo de una petición

Una petición `POST /tickets` viaja así:

1. **TraceIdFilter** (máxima precedencia): lee/genera `X-Trace-Id`, lo pone en MDC y header de respuesta.
2. **TicketController.create()**: recibe el DTO `CreateTicketRequest` validado con Bean Validation. Resuelve el `AuthenticatedActor` (header-based temporalmente; PROMPT 4 lo reemplaza con JWT). Llama a `CreateTicketUseCase.create()`.
3. **CreateTicketUseCaseImpl** (orquestación, no transaccional como conjunto):
   - **TX-1** (`CreateTicketTxAdapter`, `@Transactional`): valida que el solicitante exista y esté ACTIVO, crea el ticket `ABIERTO` con SLA deadline, guarda el ticket y el historial `CREATED`. Commit.
   - **Fuera de TX**: llama a `TechnicianDispatchPort.suggest()` (adaptador RestClient al dispatch-service). Si falla, registra la métrica pero el ticket ya está guardado.
   - **TX-2** (`AssignTicketTxAdapter`, `@Transactional`): recarga el ticket, evalúa la sugerencia del dispatch (reglas de elegibilidad + SLA), o hace fallback entre técnicos elegibles. Si hay técnico: crea `Assignment`, ticket→`ASIGNADO`, historial `ASSIGNED`. Si no: ticket queda `ABIERTO`, historial `ASSIGNMENT_FAILED`.
4. El controlador mapea el resultado a `TicketResponse` y devuelve 200.

## Entidades JPA vs dominio

Las entidades JPA (`infrastructure/adapter/out/persistence/entity/`) tienen anotaciones `@Entity`, `@Table`, `@Column`, relaciones. El dominio (`domain/model/`) no las conoce. Un mapper manual (`DomainEntityMapper`) convierte entre ambos, justificado porque el dominio usa factory methods que MapStruct no puede targetear fácilmente.

## Migraciones Flyway

- **V1__schema.sql**: tablas `users`, `technicians`, `technician_specialties`, `tickets` con constraints únicos (document, email, code, user_id) e índices (status, sla_deadline, requester_id, technician_id).
- **V2__asignaciones_historial_sla.sql**: tablas `assignments`, `ticket_history`, `sla_policies` con índices.
- **V3__datos_iniciales.sql**: solo políticas SLA por defecto (CRITICA 2h, ALTA 4h, MEDIA 8h, BAJA 24h). No inserta usuarios ni contraseñas.
- Hibernate `ddl-auto=validate` (Flyway controla el esquema).

## Transacciones

- **TX-1 y TX-2 son beans separados** para evitar self-invocation proxy bypass.
- La llamada al dispatch service está **fuera de toda transacción** para no mantener conexiones de BD abiertas durante una llamada HTTP.
- El escalamiento y la reasignación usan `@Transactional` en sus propias implementaciones.

## Manejo de errores (ProblemDetail RFC 7807)

`ApiExceptionHandler` (en `infrastructure/adapter/in/rest/handler/`):
- 400: validación, JSON malformado, parámetro inválido.
- 404: recurso no encontrado.
- 403: acceso denegado (ownership/rol).
- 409: duplicados, transición inválida, SLA inválido.
- 422: usuario inactivo, sin técnico elegible.
- 502: fallo del servicio de despacho.
- 500: error inesperado (sin stack trace).
- Todas las respuestas incluyen `type`, `title`, `status`, `detail`, `instance`, `timestamp`, `traceId`.

## No verificado

- **Tests de integración con Testcontainers**: Testcontainers 1.20.4 no detecta Docker 29.9.2 en este entorno. Los 6 tests están `@Disabled`. Comando para el propietario:
  ```bash
  ./mvnw test -Dtest=TicketFlowIntegrationTest
  ```
- **N+1 query count**: requiere Hibernate statistics en un contexto real con DB. Se verificará cuando Testcontainers funcione.

## Resultados reales

```
support-ticket-service: 79 tests (73 unit + 6 integration disabled), 0 failures — BUILD SUCCESS
technician-dispatch-service: 28 tests, 0 failures — BUILD SUCCESS
```
