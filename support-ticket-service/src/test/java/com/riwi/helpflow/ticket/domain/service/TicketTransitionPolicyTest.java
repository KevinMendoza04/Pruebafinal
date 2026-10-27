package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.exception.InvalidTicketTransitionException;
import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Parameterized tests for every legal and illegal transition,
 * role restrictions, and observation requirements.
 */
class TicketTransitionPolicyTest {

    private TicketTransitionPolicy policy;

    @BeforeEach void setUp() { policy = new TicketTransitionPolicy(); }

    private Ticket ticketIn(TicketStatus status) {
        Ticket t = Ticket.create(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC-01", Instant.now());
        // Force status via reconstitute if not ABIERTO
        if (status != TicketStatus.ABIERTO) {
            t = Ticket.reconstitute(t.getId(), t.getRequesterId(), t.getTitle(),
                    t.getDescription(), t.getCategory(), t.getPriority(),
                    t.getSite(), t.getAffectedAsset(), status, UUID.randomUUID(),
                    t.getCreatedAt(), t.getUpdatedAt(), null, null, null, null, null, null);
        }
        return t;
    }

    // ── Legal transitions ─────────────────────────────────────────────────────

    @Test @DisplayName("ABIERTO -> ASIGNADO allowed for ADMIN")
    void abiertoToAsignado_admin() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ABIERTO), TicketStatus.ASIGNADO,
                        Role.ROLE_ADMIN, false, false, null));
    }

    @Test @DisplayName("ABIERTO -> CANCELADO allowed for owner USUARIO with observation")
    void abiertoToCancelado_ownerUsuario() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ABIERTO), TicketStatus.CANCELADO,
                        Role.ROLE_USUARIO, true, false, "razón"));
    }

    @Test @DisplayName("ASIGNADO -> EN_PROGRESO allowed for assigned TECNICO")
    void asignadoToEnProgreso_assignedTecnico() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ASIGNADO), TicketStatus.EN_PROGRESO,
                        Role.ROLE_TECNICO, false, true, null));
    }

    @Test @DisplayName("EN_PROGRESO -> RESUELTO allowed for assigned TECNICO with observation")
    void enProgresoToResuelto_assignedTecnico() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.EN_PROGRESO), TicketStatus.RESUELTO,
                        Role.ROLE_TECNICO, false, true, "solucionado"));
    }

    @Test @DisplayName("RESUELTO -> CERRADO allowed for owner USUARIO")
    void resueltoCerrado_owner() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.RESUELTO), TicketStatus.CERRADO,
                        Role.ROLE_USUARIO, true, false, null));
    }

    @Test @DisplayName("RESUELTO -> EN_PROGRESO (reopen) allowed for owner USUARIO with observation")
    void resueltoReopen_ownerUsuario() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.RESUELTO), TicketStatus.EN_PROGRESO,
                        Role.ROLE_USUARIO, true, false, "reabierto"));
    }

    @Test @DisplayName("EN_PROGRESO -> CANCELADO allowed for ADMIN with observation")
    void enProgresoToCancelado_admin() {
        assertThatNoException().isThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.EN_PROGRESO), TicketStatus.CANCELADO,
                        Role.ROLE_ADMIN, false, false, "admin cancela"));
    }

    // ── Illegal transitions ───────────────────────────────────────────────────

    @Test @DisplayName("Terminal CERRADO cannot transition")
    void cerradoCannotTransition() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.CERRADO), TicketStatus.ABIERTO,
                        Role.ROLE_ADMIN, false, false, null))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("ABIERTO -> EN_PROGRESO is not in matrix")
    void abiertoToEnProgreso_illegal() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ABIERTO), TicketStatus.EN_PROGRESO,
                        Role.ROLE_ADMIN, false, false, "x"))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("ABIERTO -> CANCELADO without observation throws")
    void abiertoToCancelado_noObservation() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ABIERTO), TicketStatus.CANCELADO,
                        Role.ROLE_USUARIO, true, false, ""))
                .isInstanceOf(InvalidTicketTransitionException.class)
                .hasMessageContaining("observación");
    }

    @Test @DisplayName("USUARIO cannot transition ABIERTO -> CANCELADO on non-owned ticket")
    void abiertoToCancelado_nonOwnerUsuario() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ABIERTO), TicketStatus.CANCELADO,
                        Role.ROLE_USUARIO, false, false, "motivo"))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("Non-assigned TECNICO cannot move ASIGNADO -> EN_PROGRESO")
    void asignadoToEnProgreso_notAssignedTecnico() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.ASIGNADO), TicketStatus.EN_PROGRESO,
                        Role.ROLE_TECNICO, false, false, null))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("USUARIO cannot cancel EN_PROGRESO ticket (only ADMIN can)")
    void enProgresoToCancelado_usuario() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.EN_PROGRESO), TicketStatus.CANCELADO,
                        Role.ROLE_USUARIO, true, false, "motivo"))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("EN_PROGRESO -> RESUELTO requires observation")
    void enProgresoToResuelto_requiresObservation() {
        assertThatThrownBy(() ->
                policy.validate(ticketIn(TicketStatus.EN_PROGRESO), TicketStatus.RESUELTO,
                        Role.ROLE_TECNICO, false, true, null))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }
}
