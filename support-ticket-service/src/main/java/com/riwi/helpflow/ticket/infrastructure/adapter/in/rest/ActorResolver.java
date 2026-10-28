package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Temporary actor resolver — reads X-User-Id and X-User-Role headers.
 * PROMPT 4 replaces this with a JWT-based resolver reading the SecurityContext.
 * WHY: allows integration tests to pass actor info without real security.
 */
@Component
public class ActorResolver {

    public AuthenticatedActor resolve(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String role   = request.getHeader("X-User-Role");

        if (userId != null && role != null) {
            return new AuthenticatedActor(UUID.fromString(userId), Role.valueOf(role));
        }
        // Default to admin for now (PROMPT 4 removes this default)
        return new AuthenticatedActor(UUID.randomUUID(), Role.ROLE_ADMIN);
    }
}
