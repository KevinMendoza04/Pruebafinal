package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.handler;

import com.riwi.helpflow.ticket.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global error handler — RFC 7807 ProblemDetail.
 * Every error body has: type, title, status, detail, instance, timestamp, traceId.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE = "https://helpflow.local/problems/";

    // ── Validation (400) ─────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) detail = "Validación fallida";
        return build(HttpStatus.BAD_REQUEST, "validation-error", "Validation Error", detail, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request",
                "El cuerpo de la solicitud es inválido", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "bad-request", "Bad Request",
                "Parámetro inválido: " + ex.getName(), req);
    }

    // ── Not found (404) ───────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "not-found", "Not Found", ex.getMessage(), req);
    }

    // ── Forbidden (403) ──────────────────────────────────────────────────────
    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", ex.getMessage(), req);
    }

    // ── Conflict (409) — duplicates, invalid transitions, SLA config ────────
    @ExceptionHandler({DuplicateResourceException.class,
                       InvalidTicketTransitionException.class,
                       InvalidSlaConfigException.class,
                       TicketNotEditableException.class})
    public ProblemDetail handleConflict(DomainException ex, HttpServletRequest req) {
        HttpStatus status = (ex instanceof InvalidTicketTransitionException
                || ex instanceof TicketNotEditableException)
                ? HttpStatus.CONFLICT
                : (ex instanceof DuplicateResourceException
                    ? HttpStatus.CONFLICT
                    : HttpStatus.CONFLICT);
        return build(status, "conflict", "Conflict", ex.getMessage(), req);
    }

    // ── Inactive user (422) ──────────────────────────────────────────────────
    @ExceptionHandler(InactiveUserException.class)
    public ProblemDetail handleInactive(InactiveUserException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "inactive-user",
                "Unprocessable Entity", ex.getMessage(), req);
    }

    // ── No eligible technician (422) ────────────────────────────────────────
    @ExceptionHandler(NoEligibleTechnicianException.class)
    public ProblemDetail handleNoEligible(NoEligibleTechnicianException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "no-eligible-technician",
                "Unprocessable Entity", ex.getMessage(), req);
    }

    // ── Dispatch service error (502) ─────────────────────────────────────────
    @ExceptionHandler(DispatchServiceException.class)
    public ProblemDetail handleDispatch(DispatchServiceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, "dispatch-error",
                "Bad Gateway", "El servicio de despacho no está disponible", req);
    }

    // ── Catch-all (500) ──────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error", "Ha ocurrido un error inesperado", req);
    }

    // ── Builder ──────────────────────────────────────────────────────────────
    private ProblemDetail build(HttpStatus status, String slug, String title,
                                String detail, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE + slug));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }
}
