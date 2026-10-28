package com.riwi.helpflow.ticket.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;

/**
 * Returns 401 ProblemDetail JSON when no valid authentication is present.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException ex) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/problem+json");

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Se requiere autenticación válida");
        pd.setType(URI.create("https://helpflow.local/problems/unauthenticated"));
        pd.setTitle("Unauthorized");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("traceId", MDC.get("traceId"));

        try {
            response.getWriter().write(objectMapper.writeValueAsString(pd));
        } catch (Exception ignored) {}
    }
}
