package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the authenticated actor from the Spring Security context (populated by
 * JwtAuthenticationFilter). The domain never reads SecurityContext directly.
 */
@Component
public class ActorResolver {

    public AuthenticatedActor resolve(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof UUID userId)) {
            // Let Spring Security's AuthenticationEntryPoint handle unauthenticated access
            throw new org.springframework.security.authentication.InsufficientAuthenticationException(
                    "No authenticated actor");
        }

        // Authority is already "ROLE_XXX" (set by JWT filter), matches Role enum values
        String roleStr = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(Role.ROLE_USUARIO.name());

        return new AuthenticatedActor(userId, Role.valueOf(roleStr));
    }
}
