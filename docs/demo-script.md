# HelpFlow — Demo Script

## Prerequisites
- Docker + Docker Compose
- Postman (import `postman/helpflow-collection.json`)

## Step 1: Start services
```bash
cd Pruebafinal
docker compose up --build
```
Wait for all services to be healthy:
```bash
curl localhost:8080/actuator/health  # ticket-service
curl localhost:8081/actuator/health  # dispatch-service
```

## Step 2: Register and login
```bash
curl -X POST localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"document":"DOC-001","fullName":"Ana Lopez","email":"ana@helpflow.com","site":"BOGOTA","department":"IT","password":"pass123"}'

curl -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@helpflow.com","password":"pass123"}'
```
Save the `accessToken` from the response.

## Step 3: Create a ticket
```bash
curl -X POST localhost:8080/tickets \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -d '{"title":"Server down","description":"Production server not responding","category":"RED","priority":"ALTA","site":"BOGOTA","affectedAsset":"SRV-001"}'
```
Expected: 200 with ticket in ABIERTO or ASIGNADO status.

## Step 4: Check ticket detail and history
```bash
curl localhost:8080/tickets/<ticketId> -H 'Authorization: Bearer <TOKEN>'
curl localhost:8080/tickets/<ticketId>/history -H 'Authorization: Bearer <TOKEN>'
```

## Step 5: Transition the ticket
```bash
# To EN_PROGRESO (requires assigned technician)
curl -X POST localhost:8080/tickets/<ticketId>/transitions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -d '{"targetStatus":"EN_PROGRESO","observation":"Working on it"}'

# To RESUELTO
curl -X POST localhost:8080/tickets/<ticketId>/transitions \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <TOKEN>' \
  -d '{"targetStatus":"RESUELTO","observation":"Fixed"}'
```

## Step 6: Swagger UI
Open in browser:
- http://localhost:8080/swagger-ui.html (main service)
- http://localhost:8081/swagger-ui.html (dispatch service)

## Step 7: Metrics
```bash
curl localhost:8080/actuator/prometheus -H 'Authorization: Bearer <ADMIN_TOKEN>'
```
Look for: `helpflow_tickets_created_total`, `helpflow_tickets_overdue`, `helpflow_auth_failures_total`.

## Step 8: Dispatch service (deterministic)
```bash
curl -X POST localhost:8081/technician-dispatch \
  -H 'Content-Type: application/json' \
  -d '{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}'
```
Run twice — the output must be identical.
