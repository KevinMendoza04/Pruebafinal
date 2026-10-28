# Guía completa para mostrar y sustentar HelpFlow

## 1 — Cómo abrir el proyecto en IntelliJ

1. Abre IntelliJ IDEA
2. File → Open
3. Navega a la carpeta `Pruebafinal` y selecciona el archivo raíz (NO un subproyecto)
4. IntelliJ detectará dos `pom.xml` (uno en `support-ticket-service/` y otro en `technician-dispatch-service/`)
5. Si IntelliJ pregunta si quieres importar como proyecto Maven, di que sí
6. Si no detecta los módulos automáticamente: abre el panel Maven (lado derecho), clic en `+` y agrega ambos `pom.xml`
7. Espera a que termine de descargar dependencias (verás la barra de progreso abajo)
8. Verifica que Java 21 esté configurado: File → Project Structure → Project SDK → Java 21

## 2 — Mapa de carpetas (cómo surfear)

```
Pruebafinal/
│
├── support-ticket-service/          ← SERVICIO PRINCIPAL (80% del proyecto)
│   └── src/main/java/com/riwi/helpflow/ticket/
│       │
│       ├── domain/                  ← Núcleo del negocio, SIN Spring
│       │   ├── model/               ← Modelos: Ticket, User, Technician, etc.
│       │   ├── service/             ← Políticas: transiciones, SLA, selección
│       │   └── exception/           ← Excepciones de dominio (sin HTTP)
│       │
│       ├── application/             ← Casos de uso
│       │   ├── port/in/             ← Interfaces que los controllers implementan
│       │   ├── port/out/            ← Interfaces que los adaptadores implementan
│       │   └── usecase/             ← Implementaciones de casos de uso
│       │
│       └── infrastructure/          ← Adaptadores con tecnología real
│           ├── adapter/in/rest/     ← Controllers, DTOs, handler de errores
│           ├── adapter/out/
│           │   ├── persistence/     ← JPA entities, repos, mappers, adapters
│           │   ├── dispatch/        ← RestClient al otro microservicio
│           │   └── metrics/        ← Micrometer
│           ├── security/           ← JWT filter, SecurityConfig, BCrypt, 401/403
│           ├── config/             ← Beans, scheduler, admin bootstrap
│           └── web/                ← TraceIdFilter
│
├── technician-dispatch-service/     ← SERVICIO DE DESPACHO (simple, sin DB)
│   └── src/main/java/com/riwi/dispatch/
│       ├── domain/                  ← TechnicianDispatcher (algoritmo CRC32)
│       └── web/                     ← Controller, DTOs, handler, filter
│
├── docker-compose.yml               ← Levanta: DB + 2 servicios
├── postman/helpflow-collection.json ← Colección para Postman
└── docs/
    ├── defense/                     ← Notas de estudio
    ├── diagrams/                    ← 5 diagramas Mermaid
    └── evidence/                    ← Screenshots
```

## 3 — Qué hay en cada carpeta y qué decir

### domain/model/ — "Acá viven las entidades de negocio"

| Archivo | Qué decir |
|---|---|
| `Ticket.java` | "Aggregate raíz. `create()` crea en ABIERTO, `reconstitute()` carga desde BD. `applyTransition()` valida que no sea terminal. `escalate()` sube prioridad, idempotente." |
| `User.java` | "`register()` siempre crea ROLE_USUARIO y ACTIVO. `isActive()` es la validación antes de crear tickets." |
| `Technician.java` | "`isCompatibleWith()` verifica especialidad, `hasCapacity()` verifica que no exceda maxActiveTickets." |
| `TicketHistoryEntry.java` | "Registro inmutable: CREATED, ASSIGNED, STATUS_CHANGED, ESCALATED, ASSIGNMENT_FAILED." |
| `Assignment.java` | "`close()` marca unassignedAt durante reasignación." |

**Pregunta: "¿Dónde está la herencia?"**
→ `domain/exception/DomainException.java`. Todas las excepciones heredan de esta clase abstracta.

### domain/service/ — "Reglas de negocio puras"

| Archivo | Qué decir |
|---|---|
| `TicketTransitionPolicy.java` | "Matriz de transiciones. Switch sobre estado actual, valida rol, ownership, observación. Lanza InvalidTicketTransitionException si es ilegal." |
| `SlaCalculator.java` | "Calcula deadline = createdAt + horas(prioridad). Valida CRITICA < ALTA < MEDIA < BAJA." |
| `TechnicianSelectionPolicy.java` | "Reglas duras: activo, compatible, capacidad, sitio para ALTA/CRITICA. `selectFallback()` ordena por sitio, tickets activos, código." |
| `EscalationPolicy.java` | "Filtra tickets al 80% del SLA, no escalados, abiertos." |

### application/port/ — "Los puertos (interfaces)"

**port/in/** — "Interfaces que definen QUÉ hace el sistema. El controller las implementa."
**port/out/** — "Interfaces que definen qué necesita el dominio del exterior. Los adaptadores las implementan."

Abre `TechnicianDispatchPort.java`: "El caso de uso llama a `suggest()` sin saber que es HTTP. En tests, se reemplaza con mock."

Abre `TicketMetricsPort.java`: "Reporto eventos de métricas. El dominio no sabe que es Micrometer."

### application/usecase/ — "Orquestación"

| Archivo | Qué decir |
|---|---|
| `CreateTicketUseCaseImpl.java` | "EL más importante. TX-1 crea + commit, fuera de TX llama dispatch, TX-2 asigna. Si dispatch falla, ticket ya está guardado." |
| `AuthenticatedActor.java` | "Record con userId y role. Se construye en REST desde SecurityContext. El dominio nunca lee SecurityContext." |
| `TransitionTicketUseCaseImpl.java` | "Carga ticket, verifica ownership, delega a TicketTransitionPolicy, aplica, guarda historial." |
| `EscalateTicketsUseCaseImpl.java` | "Filtra con EscalationPolicy, llama `ticket.escalate()`, guarda ESCALATED, reintenta asignación." |

### infrastructure/adapter/in/rest/ — "Adaptadores de entrada"

| Archivo | Qué decir |
|---|---|
| `TicketController.java` | "Recibe DTOs validados, resuelve actor, delega al caso de uso. Nunca toca repositorios." |
| `TicketDtos.java` | "Records con @NotBlank, @Size. Campos en español del contrato." |
| `ApiExceptionHandler.java` | "@RestControllerAdvice. ProblemDetail RFC 7807 con traceId. Cubre 400/401/403/404/409/422/500." |
| `ActorResolver.java` | "Lee userId y role del SecurityContext (JWT filter)." |

### infrastructure/adapter/out/persistence/ — "Adaptadores JPA"

| Archivo | Qué decir |
|---|---|
| `TicketEntity.java` | "@Version para optimistic locking. @Entity, @Table. No se devuelve en respuestas REST." |
| `TicketJpaRepository.java` | "@EntityGraph evita N+1. findOpenTickets() para escalamiento." |
| `AssignTicketTxAdapter.java` | "TX-2: recarga ticket, evalúa sugerencia, lock pesimista, asigna o deja ABIERTO." |
| `DomainEntityMapper.java` | "Mapper manual dominio↔JPA. Justificado: dominio usa factory methods." |

### infrastructure/security/ — "JWT"

| Archivo | Qué decir |
|---|---|
| `JwtAuthenticationFilter.java` | "Valida Bearer token: firma HS256, expiración, usuario ACTIVO. Si falla, limpia contexto → 401." |
| `SecurityConfig.java` | "Stateless, CSRF off, @EnableMethodSecurity. /auth público, metrics ROLE_ADMIN." |
| `JwtTokenIssuerAdapter.java` | "Genera JWT con claims sub, email, role, iat, exp, jti." |

### technician-dispatch-service — "El otro microservicio"

| Archivo | Qué decir |
|---|---|
| `TechnicianDispatcher.java` | "CRC32 del string 'ticketId|categoria|prioridad'. techNum = (seed%20)+1. hours=(seed/20)%13. Sin Spring, sin DB." |

## 4 — Cómo levantarlo y demostrarlo

```bash
cd Pruebafinal
docker compose up --build
```

### Demo en vivo (orden sugerido)

1. **Swagger** (30s): abre `http://localhost:8080/swagger-ui.html`
2. **Registro + Login** (1min): POST /auth/register → POST /auth/login → copia token → Authorize
3. **Crear ticket** (1min): POST /tickets → ve status ABIERTO + slaDeadline → GET history
4. **Dispatch** (30s): abre `http://localhost:8081/swagger-ui.html` → POST 2 veces → idéntico
5. **Health** (15s): abre `http://localhost:8080/actuator/health` → DB UP

## 5 — Guión de sustentación (2-3 minutos)

> Lee esto en voz alta, sin esperar preguntas.

---

"HelpFlow es un sistema de tickets de soporte TI con dos microservicios. El servicio principal gestiona usuarios, técnicos, tickets, SLA y seguridad JWT usando arquitectura hexagonal. El servicio de despacho es externo, sin base de datos, y sugiere técnicos de forma determinista usando CRC32.

La arquitectura hexagonal separa el dominio de toda tecnología externa. **Abro la carpeta domain** — aquí están los modelos: Ticket, User, Technician. Ninguno importa Spring, JPA ni Jackson. El método `Ticket.create()` crea en ABIERTO, `applyTransition()` valida y cambia el estado, `escalate()` sube prioridad y es idempotente.

**Abro domain/service** — `TicketTransitionPolicy` codifica la matriz de transiciones. `SlaCalculator` calcula el deadline. `TechnicianSelectionPolicy` tiene las reglas duras: activo, compatible, capacidad, sitio para ALTA/CRITICA.

**Abro application/port** — los puertos. Los de entrada son interfaces que los controllers implementan. Los de salida son interfaces que los adaptadores implementan. El caso de uso no sabe si el repositorio es JPA o un mock.

**Abro application/usecase** — `CreateTicketUseCaseImpl` es el más importante. No es transaccional como conjunto. TX-1 crea el ticket y hace commit. Fuera de transacción llama al dispatch. TX-2 asigna. Si el dispatch falla, el ticket ya está guardado.

**Abro infrastructure/security** — `JwtAuthenticationFilter` valida firma, expiración y que el usuario siga ACTIVO. `SecurityConfig` configura stateless y `@PreAuthorize`. Si un usuario intenta ver ticket ajeno, recibe 403.

**Abro infrastructure/adapter/out/persistence** — entidades JPA separadas del dominio. `@Version` para optimistic locking. `@EntityGraph` evita N+1. Mapper manual convierte entre dominio y JPA.

**Abro technician-dispatch-service** — `TechnicianDispatcher`: CRC32 del string, techNum = (seed%20)+1. Determinista, sin Spring, sin DB.

Tests: 110 en total, 0 fallos. ArchUnit verifica que el dominio no dependa de Spring ni JPA."

---

## 6 — Las 10 preguntas que te van a hacer

**1. ¿Por qué hexagonal y no MVC en capas?**
"En MVC en capas el dominio termina acoplado al framework. En hexagonal, el dominio no sabe que Spring existe. Lo pruebo sin contexto de Spring. ArchUnit lo verifica."

**2. ¿Qué es un puerto?**
"Una interfaz. Puerto de entrada: contrato que el controller cumple. Puerto de salida: contrato que el adaptador implementa. El dominio define los puertos de salida."

**3. ¿Por qué dos transacciones?**
"La llamada HTTP al dispatch no puede mantener transacción abierta. TX-1 crea y commitea. Si el dispatch falla, el ticket ya está seguro. TX-2 solo asigna."

**4. ¿Cómo evitas IDOR?**
"En `GetTicketsUseCaseImpl.checkAccess()` verifico que el USUARIO solo acceda a tickets donde `requesterId == actor.userId()`. Check contra datos persistidos. Si falla, 403, no 404."

**5. ¿Qué pasa si el dispatch está caído?**
"Lanza DispatchServiceException. El caso de uso lo captura, registra métrica, TX-2 recibe null. Intenta fallback. Si no hay elegible, ticket queda ABIERTO con ASSIGNMENT_FAILED. Nunca se pierde."

**6. ¿Cómo funciona el escalamiento?**
"Scheduler cada 60s. Filtra tickets al 80% del SLA, no escalados, abiertos. `Ticket.escalate()` sube prioridad un nivel y marca escalatedAt. Idempotente: si ya escalado, no hace nada."

**7. ¿Qué es @Version?**
"Optimistic locking. Si dos usuarios cambian el mismo ticket a la vez, Hibernate detecta el conflicto y lanza OptimisticLockException."

**8. ¿Por qué el dominio no usa @Entity?**
"Porque JPA es infraestructura. Si el dominio importara @Entity, no podría probarse sin base de datos. ArchUnit lo verifica."

**9. ¿Por qué CRC32 y no Random?**
"CRC32 produce el mismo valor para la misma secuencia de bytes en cualquier JVM. Random no es determinista sin semilla fija. hashCode() de String no es estable entre versiones."

**10. ¿Qué métricas tienes?**
"Nueve: tickets.created (counter), first_response (timer), resolution (timer), overdue (gauge), sla.breaches (counter), escalated (counter), auth.failures (counter), access.denied (counter), dispatch.failures (counter). El gauge mide estado actual; los demás son acumulativos."
