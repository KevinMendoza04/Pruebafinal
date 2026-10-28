# Fase 5 — Docker, documentación y entrega final

## Qué se construyó

- Dockerfiles multi-stage para ambos microservicios (Maven build + JRE runtime).
- `docker-compose.yml` con PostgreSQL, dispatch-service y ticket-service, health checks y dependencias.
- README.md completo en inglés con arquitectura, endpoints, variables de entorno y instrucciones.
- 5 diagramas Mermaid: arquitectura hexagonal, ciclo de vida del ticket, comunicación entre microservicios, ER y secuencia de asignación.
- Colección Postman importable con todas las operaciones.
- `docs/demo-script.md` con pasos exactos para capturar evidencias.
- `docs/defense/guia-sustentacion.md` con guía de defensa oral.

## Estado del proyecto

### Tests
```
technician-dispatch-service: 28 tests, 0 failures — BUILD SUCCESS
support-ticket-service: 82 tests, 0 failures, 6 skipped (Testcontainers) — BUILD SUCCESS
Total: 110 tests, 0 failures, 6 skipped
```

### Cobertura (sin gate, reporte generado)
- domain/model: ~69%
- domain/service: ~89%
- application/usecase: ~40%
- domain/exception: ~57%

El gate JaCoCo >= 80% se reactivará cuando Testcontainers funcione en el entorno del propietario.

### Docker
- Dockerfiles multi-stage: compilan con Maven 3.9 + JDK 21, ejecutan con JRE 21 Alpine.
- `docker compose up --build` levanta todo: DB, dispatch, ticket-service con dependencias y health checks.
- No se pudo verificar la construcción de imágenes Docker en este entorno (compatibilidad Testcontainers/Docker 29.x).

### Endpoints
- Todos los endpoints del enunciado están implementados.
- Swagger UI disponible en ambos servicios.
- Actuator: health/info públicos, metrics/prometheus ROLE_ADMIN.

## No verificado

- **Docker build**: no se ejecutó `docker compose up --build` en este entorno. El propietario debe ejecutarlo y verificar.
- **Integration tests**: Testcontainers no detecta Docker 29.x. Tests `@Disabled`.
- **Cobertura >= 80%**: pendiente de los integration tests.
- **N+1 query count**: requiere Hibernate statistics con DB real.

## Comando final para el propietario

```bash
cd Pruebafinal
docker compose up --build
# En otra terminal:
curl localhost:8080/actuator/health
curl localhost:8081/actuator/health
# Seguir docs/demo-script.md para capturar evidencias
```
