package com.riwi.helpflow.ticket.domain.model;

import com.riwi.helpflow.ticket.domain.exception.InvalidTicketTransitionException;
import com.riwi.helpflow.ticket.domain.exception.TicketNotEditableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TicketTest {

    private Ticket freshTicket() {
        return Ticket.create(UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Description long enough", TicketCategory.RED,
                TicketPriority.ALTA, "BOGOTA", "PC-01", Instant.now());
    }

    @Test @DisplayName("New ticket has status ABIERTO")
    void newTicket_isAbierto() {
        assertThat(freshTicket().getStatus()).isEqualTo(TicketStatus.ABIERTO);
    }

    @Test @DisplayName("applyTransition changes status correctly")
    void applyTransition_changesStatus() {
        Ticket t = freshTicket();
        t.applyTransition(TicketStatus.ASIGNADO, Instant.now());
        assertThat(t.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
    }

    @Test @DisplayName("applyTransition to EN_PROGRESO sets firstRespondedAt")
    void applyTransition_setsFirstRespondedAt() {
        Ticket t = freshTicket();
        t.assignTo(UUID.randomUUID(), Instant.now());
        Instant before = Instant.now();
        t.applyTransition(TicketStatus.EN_PROGRESO, before);
        assertThat(t.getFirstRespondedAt()).isEqualTo(before);
    }

    @Test @DisplayName("applyTransition to RESUELTO sets resolvedAt")
    void applyTransition_setsResolvedAt() {
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC", TicketStatus.EN_PROGRESO, UUID.randomUUID(),
                Instant.now(), Instant.now(), null, Instant.now(), null, null, null, null);
        Instant now = Instant.now();
        t.applyTransition(TicketStatus.RESUELTO, now);
        assertThat(t.getResolvedAt()).isEqualTo(now);
    }

    @Test @DisplayName("Cannot transition from terminal status")
    void terminalTransition_throws() {
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC", TicketStatus.CERRADO, null,
                Instant.now(), Instant.now(), null, null, null, null, null, null);
        assertThatThrownBy(() -> t.applyTransition(TicketStatus.ABIERTO, Instant.now()))
                .isInstanceOf(InvalidTicketTransitionException.class);
    }

    @Test @DisplayName("escalate raises priority once and sets escalatedAt")
    void escalate_raisesPriority() {
        Ticket t = freshTicket(); // priority ALTA
        boolean escalated = t.escalate(Instant.now());
        assertThat(escalated).isTrue();
        assertThat(t.getPriority()).isEqualTo(TicketPriority.CRITICA);
        assertThat(t.getEscalatedAt()).isNotNull();
    }

    @Test @DisplayName("escalate on CRITICA stays CRITICA")
    void escalate_criticaStaysCritica() {
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.CRITICA,
                "BOGOTA", "PC", TicketStatus.ABIERTO, null,
                Instant.now(), Instant.now(), null, null, null, null, null, null);
        t.escalate(Instant.now());
        assertThat(t.getPriority()).isEqualTo(TicketPriority.CRITICA);
    }

    @Test @DisplayName("escalate is idempotent — second call returns false")
    void escalate_idempotent() {
        Ticket t = freshTicket();
        t.escalate(Instant.now());
        assertThat(t.escalate(Instant.now())).isFalse();
    }

    @Test @DisplayName("updateFields fails when not ABIERTO")
    void updateFields_notAbierto_throws() {
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC", TicketStatus.ASIGNADO, UUID.randomUUID(),
                Instant.now(), Instant.now(), null, null, null, null, null, null);
        assertThatThrownBy(() -> t.updateFields("New", "Desc new long enough", "asset", Instant.now()))
                .isInstanceOf(TicketNotEditableException.class);
    }

    @Test @DisplayName("isEligibleForEscalation respects threshold boundary")
    void escalationEligibility_boundary() {
        Instant created  = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = created.plusSeconds(10 * 3600);
        Instant at80pct  = created.plusSeconds(8 * 3600);
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", "PC", TicketStatus.ABIERTO, null,
                created, created, deadline, null, null, null, null, null);
        assertThat(t.isEligibleForEscalation(at80pct, 80)).isTrue();
        assertThat(t.isEligibleForEscalation(at80pct.minusSeconds(1), 80)).isFalse();
    }
}
