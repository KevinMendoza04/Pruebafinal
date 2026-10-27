package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EscalationPolicyTest {

    private EscalationPolicy policy;

    @BeforeEach void setUp() { policy = new EscalationPolicy(); }

    private Ticket ticketWithSla(TicketStatus status, Instant createdAt, Instant slaDeadline,
                                 boolean alreadyEscalated) {
        Ticket t = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", "PC-01", status, null,
                createdAt, createdAt, slaDeadline, null, null,
                alreadyEscalated ? createdAt : null, null, null);
        return t;
    }

    @Test @DisplayName("Ticket at 80% of SLA window is eligible")
    void eligibleAt80Percent() {
        Instant created  = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = created.plusSeconds(10 * 3600);    // 10h window
        Instant now      = created.plusSeconds(8 * 3600);     // 80% elapsed
        Ticket t = ticketWithSla(TicketStatus.ABIERTO, created, deadline, false);
        assertThat(policy.findEligible(List.of(t), now, 80)).hasSize(1);
    }

    @Test @DisplayName("Ticket at 79% of SLA window is NOT eligible")
    void notEligibleAt79Percent() {
        Instant created  = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = created.plusSeconds(10 * 3600);
        Instant now      = created.plusSeconds(7 * 3600 + 3559); // ~79.9%
        Ticket t = ticketWithSla(TicketStatus.ABIERTO, created, deadline, false);
        assertThat(policy.findEligible(List.of(t), now, 80)).isEmpty();
    }

    @Test @DisplayName("Already escalated ticket is not eligible again — idempotency")
    void alreadyEscalated_notEligible() {
        Instant created  = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = created.plusSeconds(10 * 3600);
        Instant now      = created.plusSeconds(9 * 3600); // 90%
        Ticket t = ticketWithSla(TicketStatus.ABIERTO, created, deadline, true); // already escalated
        assertThat(policy.findEligible(List.of(t), now, 80)).isEmpty();
    }

    @Test @DisplayName("Terminal ticket (CERRADO) is not eligible for escalation")
    void terminalTicket_notEligible() {
        Instant created  = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = created.plusSeconds(4 * 3600);
        Instant now      = created.plusSeconds(4 * 3600); // 100%
        Ticket t = ticketWithSla(TicketStatus.CERRADO, created, deadline, false);
        assertThat(policy.findEligible(List.of(t), now, 80)).isEmpty();
    }
}
