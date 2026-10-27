# Fase 1 — technician-dispatch-service

## Qué se construyó y por qué

El servicio `technician-dispatch-service` es el microservicio externo simulado de HelpFlow.
Recibe un ticket (id, categoría, prioridad) y devuelve un técnico sugerido y un tiempo
estimado de atención de forma **determinista** (la misma entrada siempre produce la misma salida).

No tiene base de datos, no tiene seguridad JWT: es un servicio auxiliar cuyo único propósito
es simular un sistema externo de despacho.

## Dónde vive

| Elemento | Ruta exacta |
|---|---|
| Algoritmo puro | `technician-dispatch-service/src/main/java/com/riwi/dispatch/domain/TechnicianDispatcher.java` |
| Enums del dominio | `domain/TicketCategory`, `domain/TicketPriority`, `domain/Availability` |
| Value object resultado | `domain/DispatchResult` (Java record) |
| DTOs de la API | `web/dto/DispatchRequest`, `web/dto/DispatchResponse` (Java records) |
| Controlador | `web/controller/DispatchController.dispatch()` |
| Filtro traceId | `web/filter/TraceIdFilter` |
| Manejador de errores | `web/handler/GlobalExceptionHandler` |
| Configuración beans | `web/config/AppConfig` (registra `TechnicianDispatcher`) |
| Pruebas dominio | `src/test/.../domain/TechnicianDispatcherTest` (14 casos) |
| Pruebas web | `src/test/.../web/DispatchControllerTest` (14 casos) |

## Cómo fluye la ejecución paso a paso

1. **Petición HTTP**: `POST /technician-dispatch` con `{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}`.
2. **TraceIdFilter** (precedencia máxima): lee `X-Trace-Id` del header; si es válido (8–64 chars alfanuméricos y guiones) lo reutiliza; si no, genera un UUID. Lo pone en MDC y lo escribe en el header de respuesta.
3. **Bean Validation** (`@Valid`): verifica que `ticketId > 0`, `categoria` y `prioridad` no sean nulos y sean valores del enum. Si falla → `GlobalExceptionHandler.handleValidation()` → 400 ProblemDetail.
4. **DispatchController.dispatch()**: construye un `DispatchRequest` y llama a `TechnicianDispatcher.dispatch(ticketId, categoria, prioridad)`.
5. **TechnicianDispatcher** (dominio puro, sin Spring):
   - Construye la cadena `"125|RED|ALTA"` en UTF-8.
   - Calcula `CRC32` → valor `long` sin signo (0..4 294 967 295).
   - `techNum = (seed % 20) + 1` → ej. `TEC-008`.
   - `hours = (seed / 20) % 13` → 0..12.
   - `availability`: `hours ≤ 2` → ALTA, `≤ 8` → MEDIA, `> 8` → BAJA.
   - Devuelve un `DispatchResult` (record inmutable).
6. **DispatchController** mapea el resultado a `DispatchResponse` y Spring serializa a JSON.
7. **Respuesta 200**: `{"ticketId":125,"tecnicoId":"TEC-008","disponibilidad":"BAJA","tiempoEstimadoHoras":12}`.

## 5 preguntas probables en la defensa

1. **¿Por qué `TechnicianDispatcher` no tiene `@Component`?**
   Porque es una clase de dominio puro: no debe depender de Spring. Se registra como bean en `AppConfig` (capa de infraestructura), respetando la regla de que el dominio no importa ningún framework.

2. **¿Por qué se usa CRC32 y no `Random` o `hashCode()`?**
   `Random` no es determinista entre ejecuciones sin una semilla fija. `hashCode()` de String no está garantizado entre JVMs ni entre versiones de Java. CRC32 de `java.util.zip` produce siempre el mismo valor para la misma secuencia de bytes, en cualquier JVM, en cualquier plataforma.

3. **¿Qué pasa si llega un JSON con `"categoria":"OTRO_VALOR"`?**
   Jackson no puede deserializar el valor al enum `TicketCategory`. Lanza `HttpMessageNotReadableException`. `GlobalExceptionHandler.handleUnreadable()` lo captura y devuelve un 400 ProblemDetail con `traceId` y `timestamp`.

4. **¿Por qué los campos del JSON están en español (`tecnicoId`, `disponibilidad`, `tiempoEstimadoHoras`)?**
   El contrato del enunciado define exactamente esos nombres. Cambiarlos a inglés rompería la integración con `support-ticket-service`. Los nombres de las clases Java y los métodos sí están en inglés.

5. **¿Cómo se verifica el determinismo en las pruebas?**
   `TechnicianDispatcherTest.determinism_sameInputAlwaysSameOutput()` llama al dispatcher 100 veces con la misma entrada y compara todos los resultados con el primero. Si hubiera aleatoriedad, el test fallaría tarde o temprano.

## 2 ejercicios para el propietario

1. Abre `TechnicianDispatcher.java` y calcula manualmente el resultado para `ticketId=1, categoria=HARDWARE, prioridad=BAJA`. Usa una calculadora CRC32 online con la cadena `"1|HARDWARE|BAJA"` y verifica que el servicio arroja lo mismo.

2. Envía una petición con `X-Trace-Id: mi-trace-id-12345` y observa el header de respuesta. Luego envía con `X-Trace-Id: abc` (demasiado corto) y observa que el servidor lo ignora y genera uno propio.
