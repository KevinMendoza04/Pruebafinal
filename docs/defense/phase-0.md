# Fase 0 — Verificación del entorno y checklist de trazabilidad

## Qué se construyó y por qué

Esta fase no genera código de producción. Su objetivo es:
- Confirmar que el entorno de desarrollo cumple los requisitos mínimos.
- Crear los artefactos de trazabilidad y documentación que guiarán el resto del proyecto.

## Dónde vive

| Artefacto | Ruta |
|---|---|
| Tabla de trazabilidad | `docs/assessment-checklist.md` |
| Notas de diseño | `docs/design-notes.md` |
| Carpetas vacías con `.gitkeep` | `docs/diagrams/`, `docs/evidence/`, `docs/defense/`, `postman/`, `scripts/` |
| Actualización `.gitignore` | `.gitignore` (se añadió `dist/`, IDE: `.classpath`, `.project`, `.settings/`, `bin/`, `nbproject/`) |

## Resultados del entorno

| Herramienta | Versión detectada |
|---|---|
| Java | OpenJDK 21.0.12.1 |
| Maven | 3.8.7 |
| Docker | 29.9.2 |
| Docker Compose | v5.6.1 |
| Daemon Docker | activo (2 contenedores stopped) |
| Rama git | `main` |
| Remote | `https://github.com/KevinMendoza04/Pruebafinal` |

## Por qué arquitectura hexagonal

La arquitectura hexagonal (Ports & Adapters, Alistair Cockburn, 2005) separa el núcleo del negocio
de cualquier tecnología externa. Los beneficios concretos para este proyecto son:

1. **Dominio puro**: `User`, `Ticket`, `TicketTransitionPolicy`, etc. no importan Spring ni JPA.
   Se pueden probar con JUnit sin levantar ningún contexto.
2. **Puertos (interfaces)**: definen el contrato que el dominio/aplicación necesita del mundo exterior
   (`TicketRepositoryPort`, `TechnicianDispatchPort`, `TicketMetricsPort`…).
3. **Adaptadores**: implementan esos contratos con tecnología real (JPA, RestClient, Micrometer).
   Cambiar de PostgreSQL a otra base sólo afecta a los adaptadores.
4. **Testa bilidad**: ArchUnit verifica en tiempo de compilación que las reglas de dependencia no se rompan.
5. **Obligatorio en la rúbrica** (Architecture 20 pts): la rúbrica exige "dominio puro sin dependencias
   de frameworks" y "puertos bien abstraídos".

## Cómo se organiza el repositorio

```
Pruebafinal/
├── support-ticket-service/      ← servicio principal (Java 21, Spring Boot 3.5)
│   └── src/main/java/com/riwi/helpflow/ticket/
│       ├── domain/              ← modelo rico, políticas, excepciones de negocio
│       ├── application/         ← casos de uso + puertos (in/out)
│       └── infrastructure/      ← adaptadores REST, JPA, seguridad, métricas
├── technician-dispatch-service/ ← servicio externo simulado (sin JPA, sin seguridad)
├── docs/                        ← diagramas, evidencias, defensa, checklist
├── postman/                     ← colección y entorno (sin secretos)
├── scripts/                     ← demo.sh, make-zip.sh
├── docker-compose.yml           ← orquestación local
└── .env.example                 ← plantilla de variables de entorno
```

## Flujo de ejecución (arranque)

1. `docker compose up` levanta `db` (PostgreSQL 16), `dispatch-service` (puerto 8081) y `ticket-service` (puerto 8080).
2. Flyway aplica `V1`, `V2`, `V3` al iniciar `ticket-service` y valida el esquema con `ddl-auto=validate`.
3. El `AdminBootstrapper` crea el admin si no existe (variables de entorno).
4. El perfil `demo` (opcional) siembra los 20 técnicos `TEC-001..TEC-020`.

## 5 preguntas probables en la defensa

1. **¿Por qué el dominio no puede importar `@Entity` de JPA?**
   Porque JPA es un detalle de infraestructura. Si el dominio dependiera de él, no podría probarse sin base de datos y estaría acoplado a una tecnología específica.

2. **¿Qué es un puerto de salida? Dé un ejemplo.**
   Es una interfaz definida en la capa de aplicación que el dominio usa para pedir algo al exterior. Ejemplo: `TechnicianDispatchPort.suggest(ticketId, category, priority)`. El adaptador REST lo implementa; el test lo reemplaza con un mock.

3. **¿Por qué se usan dos transacciones separadas (TX-1 y TX-2) al crear un ticket?**
   Para que la llamada HTTP al servicio de despacho quede fuera de cualquier transacción abierta. Si TX-1 crea el ticket y hace commit, el ticket no se pierde aunque el despacho falle. TX-2 sólo asigna.

4. **¿Qué ventaja aporta ArchUnit al proyecto?**
   Convierte las reglas de arquitectura en pruebas automatizadas. Si alguien inyecta un repositorio directamente en un controlador, el build falla antes de llegar a producción.

5. **¿Cómo garantiza el proyecto que no se escriben secretos en el repositorio?**
   `.env` está en `.gitignore`. `.env.example` sólo contiene placeholders. Antes de cada commit se revisan los archivos staged. `JWT_SECRET` y contraseñas se leen únicamente de variables de entorno.

## 2 ejercicios para el propietario

1. Abre `docs/assessment-checklist.md` y localiza los IDs `ARCH-01` a `ARCH-03`. Explica en voz alta qué clase o anotación viola cada regla y cómo ArchUnit la detectaría.
2. Dibuja en papel el flujo completo de creación y asignación de un ticket, marcando dónde empieza TX-1, dónde ocurre la llamada HTTP y dónde empieza TX-2. Identifica qué pasa si TX-2 falla.
