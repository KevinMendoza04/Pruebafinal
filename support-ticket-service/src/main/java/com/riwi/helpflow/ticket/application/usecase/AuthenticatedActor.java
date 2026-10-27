package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.domain.model.Role;

import java.util.UUID;

/**
 * Carries the authenticated actor's identity into use cases.
 * Built by the REST adapter from the security context.
 * The domain never reads the SecurityContext directly.
 */
public record AuthenticatedActor(UUID userId, Role role) {
    public boolean isAdmin()     { return role == Role.ROLE_ADMIN; }
    public boolean isTechnician(){ return role == Role.ROLE_TECNICO; }
    public boolean isUser()      { return role == Role.ROLE_USUARIO; }
}
