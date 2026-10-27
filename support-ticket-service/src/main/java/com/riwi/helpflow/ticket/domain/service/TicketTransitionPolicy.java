package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.exception.InvalidTicketTransitionException;
import com.riwi.helpflow.ticket.domain.model.Role;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;

import java.util.Set;

/**
 * Encodes the legal transition matrix and role/observation rules from business-rules.md.
 *
 * Transition matrix:
 *   ABIERTO     -> ASIGNADO       (system/ADMIN)           — observation optional
 *   ABIERTO     -> CANCELADO      (owner USUARIO, ADMIN)   — observation required
 *   ASIGNADO    -> EN_PROGRESO    (assigned TECNICO, ADMIN) — observation optional
 *   ASIGNADO    -> CANCELADO      (owner USUARIO, ADMIN)   — observation required
 *   EN_PROGRESO -> RESUELTO       (assigned TECNICO, ADMIN) — observation required
 *   EN_PROGRESO -> CANCELADO      (ADMIN only)             — observation required
 *   RESUELTO    -> CERRADO        (owner USUARIO, ADMIN)   — observation optional
 *   RESUELTO    -> EN_PROGRESO    (assigned TECNICO, owner USUARIO, ADMIN) — observation required
 *
 * CERRADO and CANCELADO are terminal; no transition out is possible.
 */
public class TicketTransitionPolicy {

    /**
     * Validates that the requested transition is legal for the given actor.
     * Throws InvalidTicketTransitionException with a descriptive Spanish message if not.
     *
     * @param ticket       current ticket
     * @param targetStatus desired new status
     * @param actorRole    role of the authenticated actor
     * @param isOwner      true if the actor is the ticket requester
     * @param isAssigned   true if the actor is the currently assigned technician
     * @param observation  caller-supplied observation (may be null/blank)
     */
    public void validate(Ticket ticket, TicketStatus targetStatus,
                         Role actorRole, boolean isOwner, boolean isAssigned,
                         String observation) {
        TicketStatus current = ticket.getStatus();

        if (current.isTerminal()) {
            throw new InvalidTicketTransitionException(current, targetStatus,
                    "El ticket está en estado terminal");
        }

        switch (current) {
            case ABIERTO -> validateFromAbierto(targetStatus, actorRole, isOwner, observation);
            case ASIGNADO -> validateFromAsignado(targetStatus, actorRole, isOwner, isAssigned, observation);
            case EN_PROGRESO -> validateFromEnProgreso(targetStatus, actorRole, isOwner, isAssigned, observation);
            case RESUELTO -> validateFromResuelto(targetStatus, actorRole, isOwner, isAssigned, observation);
            default -> throw new InvalidTicketTransitionException(current, targetStatus,
                    "Estado de origen no soportado en la política de transición");
        }
    }

    private void validateFromAbierto(TicketStatus target, Role role,
                                     boolean isOwner, String observation) {
        switch (target) {
            case ASIGNADO -> requireRole(target, role, Set.of(Role.ROLE_ADMIN));
            case CANCELADO -> {
                requireRole(target, role, Set.of(Role.ROLE_USUARIO, Role.ROLE_ADMIN));
                requireOwnerOrAdmin(target, role, isOwner);
                requireObservation(target, observation);
            }
            default -> illegalTransition(TicketStatus.ABIERTO, target);
        }
    }

    private void validateFromAsignado(TicketStatus target, Role role,
                                      boolean isOwner, boolean isAssigned,
                                      String observation) {
        switch (target) {
            case EN_PROGRESO -> {
                requireRole(target, role, Set.of(Role.ROLE_TECNICO, Role.ROLE_ADMIN));
                if (role == Role.ROLE_TECNICO && !isAssigned) {
                    throw new InvalidTicketTransitionException(TicketStatus.ASIGNADO, target,
                            "El técnico no está asignado a este ticket");
                }
            }
            case CANCELADO -> {
                requireRole(target, role, Set.of(Role.ROLE_USUARIO, Role.ROLE_ADMIN));
                requireOwnerOrAdmin(target, role, isOwner);
                requireObservation(target, observation);
            }
            default -> illegalTransition(TicketStatus.ASIGNADO, target);
        }
    }

    private void validateFromEnProgreso(TicketStatus target, Role role,
                                        boolean isOwner, boolean isAssigned,
                                        String observation) {
        switch (target) {
            case RESUELTO -> {
                requireRole(target, role, Set.of(Role.ROLE_TECNICO, Role.ROLE_ADMIN));
                if (role == Role.ROLE_TECNICO && !isAssigned) {
                    throw new InvalidTicketTransitionException(TicketStatus.EN_PROGRESO, target,
                            "El técnico no está asignado a este ticket");
                }
                requireObservation(target, observation);
            }
            case CANCELADO -> {
                requireRole(target, role, Set.of(Role.ROLE_ADMIN));
                requireObservation(target, observation);
            }
            default -> illegalTransition(TicketStatus.EN_PROGRESO, target);
        }
    }

    private void validateFromResuelto(TicketStatus target, Role role,
                                      boolean isOwner, boolean isAssigned,
                                      String observation) {
        switch (target) {
            case CERRADO -> {
                requireRole(target, role, Set.of(Role.ROLE_USUARIO, Role.ROLE_ADMIN));
                requireOwnerOrAdmin(target, role, isOwner);
            }
            case EN_PROGRESO -> {
                // reopen
                requireRole(target, role,
                        Set.of(Role.ROLE_TECNICO, Role.ROLE_USUARIO, Role.ROLE_ADMIN));
                if (role == Role.ROLE_TECNICO && !isAssigned) {
                    throw new InvalidTicketTransitionException(TicketStatus.RESUELTO, target,
                            "El técnico no está asignado a este ticket");
                }
                if (role == Role.ROLE_USUARIO && !isOwner) {
                    throw new InvalidTicketTransitionException(TicketStatus.RESUELTO, target,
                            "El usuario no es propietario del ticket");
                }
                requireObservation(target, observation);
            }
            default -> illegalTransition(TicketStatus.RESUELTO, target);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void requireRole(TicketStatus target, Role actual, Set<Role> allowed) {
        if (!allowed.contains(actual)) {
            throw new InvalidTicketTransitionException(null, target,
                    "El rol " + actual + " no puede hacer esta transición");
        }
    }

    private void requireOwnerOrAdmin(TicketStatus target, Role role, boolean isOwner) {
        if (role != Role.ROLE_ADMIN && !isOwner) {
            throw new InvalidTicketTransitionException(null, target,
                    "Solo el propietario o un administrador puede realizar esta acción");
        }
    }

    private void requireObservation(TicketStatus target, String observation) {
        if (observation == null || observation.isBlank()) {
            throw new InvalidTicketTransitionException(null, target,
                    "Se requiere una observación para esta transición");
        }
    }

    private void illegalTransition(TicketStatus from, TicketStatus to) {
        throw new InvalidTicketTransitionException(from, to,
                "Transición no permitida según la matriz de estados");
    }
}
