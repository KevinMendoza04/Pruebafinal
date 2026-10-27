package com.riwi.dispatch.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reads X-Trace-Id from the incoming request (if it matches the allowed pattern),
 * otherwise generates a new UUID. Stores it in MDC and echoes it back in the response header.
 *
 * WHY: Every ProblemDetail must include a traceId so the caller can correlate errors
 * across services. Highest precedence ensures all downstream filters and handlers see the value.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String MDC_KEY      = "traceId";

    // Allowed pattern: 8–64 characters, alphanumeric and hyphens only
    private static final Pattern VALID_TRACE = Pattern.compile("^[A-Za-z0-9\\-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_HEADER);
        String traceId = (incoming != null && VALID_TRACE.matcher(incoming).matches())
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
