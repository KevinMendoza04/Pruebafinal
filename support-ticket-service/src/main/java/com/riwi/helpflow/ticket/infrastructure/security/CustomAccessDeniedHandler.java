package com.riwi.helpflow.ticket.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riwi.helpflow.ticket.application.port.out.TicketMetricsPort;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.metrics.MicrometerTicketMetricsAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;

/**
 * Returns 403 ProblemDetail JSON when an authenticated user lacks permissions.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final TicketMetricsPort metrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomAccessDeniedHandler(TicketMetricsPort metrics) {
        this.metrics = metrics;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException ex) {
        metrics.recordAccessDenied();
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Acceso denegado: no tiene permisos para esta operación");
        pd.setType(URI.create("https://helpflow.local/problems/forbidden"));
        pd.setTitle("Forbidden");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("traceId", MDC.get("traceId"));

        try {
            response.getWriter().write(objectMapper.writeValueAsString(pd));
        } catch (Exception ignored) {}
    }
}
