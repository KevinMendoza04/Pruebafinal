package com.riwi.dispatch.web.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised error handler returning RFC-7807 ProblemDetail for all 4xx/5xx responses.
 * Every error body includes: type, title, status, detail, instance, timestamp, traceId.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://helpflow.local/problems/";

    // ── Bean Validation failures (missing fields, @Positive, @NotNull) ────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, "validation-error",
                "Validation Error", detail, request);
    }

    // ── Malformed JSON or unknown enum value ──────────────────────────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex,
                                          HttpServletRequest request) {
        // Keep detail brief — never leak internal stack traces
        String detail = "El cuerpo de la solicitud es inválido o contiene un valor de enumeración desconocido";
        return build(HttpStatus.BAD_REQUEST, "bad-request",
                "Bad Request", detail, request);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal Server Error",
                "Ha ocurrido un error inesperado", request);
    }

    // ── Builder ───────────────────────────────────────────────────────────────
    private ProblemDetail build(HttpStatus status, String slug, String title,
                                String detail, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(BASE_TYPE + slug));
        pd.setTitle(title);
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("traceId", MDC.get("traceId"));
        return pd;
    }
}
