# Fase 1 — Análisis, reglas del proyecto y planificación

## Qué exige el enunciado

HelpFlow es un sistema de tickets de soporte TI con dos microservicios:
- **support-ticket-service**: gestión de usuarios, técnicos, tickets, SLA, asignación, escalamiento, historial, seguridad JWT, métricas. Arquitectura hexagonal.
- **technician-dispatch-service**: servicio externo simulado que sugiere un técnico y un tiempo estimado de atención. Determinista, sin base de datos ni seguridad.

La evaluación valora: arquitectura hexagonal pura (20), funcionalidad (20), seguridad (20), calidad/pruebas (20), sustentación (10), documentación (10).

## Qué es SDD (Spec-Driven Development) y cómo se usa

SDD significa escribir primero las especificaciones (requisitos, diseño y tareas) antes de programar. En Kiro esto se hace con el flujo nativo de Specs. Aquí lo aplicamos con:
- `docs/assessment-checklist.md`: matriz de trazabilidad (requisito → ubicación → verificación → estado).
- `docs/design-notes.md`: supuestos y decisiones de arquitectura (ADR).
- Steering files (locales, no commiteados) que fijan las reglas vinculantes.

## Por qué existen dos microservicios

La arquitectura de microservicios separa la responsabilidad:
- El servicio principal contiene toda la lógica de negocio y los datos.
- El servicio de despacho simula un sistema externo de recomendación. Es independiente, no comparte base de datos y se puede reemplazar sin tocar el dominio.
- Se comunican por REST, lo que demuestra integración entre microservicios (criterio de la rúbrica).

## Qué significa arquitectura hexagonal

La arquitectura hexagonal (Ports & Adapters) aísla el núcleo del negocio de toda tecnología externa:
- **Dominio puro**: modelos ricos (`User`, `Ticket`, `Technician`) y servicios de dominio (`TicketTransitionPolicy`, `SlaCalculator`). No importan Spring, JPA ni Jackson.
- **Puertos**: interfaces que definen lo que el dominio necesita del exterior (repositorios, despacho, métricas, hasher).
- **Adaptadores**: implementaciones con tecnología real (JPA, RestClient, Micrometer, BCrypt).
- **Casos de uso**: orquestan el dominio a través de los puertos, sin tocar infraestructura.

Esto permite probar el dominio sin base de datos ni contexto de Spring.

## Decisiones fijadas

- Java 21, Spring Boot 3.5.x, Maven 3.9+ (via wrapper).
- Enums en inglés; valores de enum y campos del contrato JSON del despacho en español.
- SLA iniciales: CRITICA 2h, ALTA 4h, MEDIA 8h, BAJA 24h (valores de diseño).
- Escalamiento al 80% del SLA, una sola vez por ticket, CRITICA no sube.
- Registro público solo crea ROLE_USUARIO.
- Testcontainers con PostgreSQL (no H2).
- `.kiro/` no se commitea (sin marcas de IA en el repo).

## Supuestos

Ver `docs/design-notes.md` sección "Assumptions".
