package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.EscalationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalateTicketsUseCaseTest {

    @Mock TicketRepositoryPort        ticketRepo;
    @Mock TicketHistoryRepositoryPort historyRepo;
    @Mock AssignTicketTxPort          assignTx;
    @Mock TicketMetricsPort           metrics;

    EscalateTicketsUseCaseImpl useCase;
    Clock                      fixedClock;

    Instant now      = Instant.parse("2025-01-01T10:00:00Z");
    Instant created  = now.minusSeconds(9 * 3600);    // 9h ago
    Instant deadline = created.plusSeconds(10 * 3600); // 10h SLA → 80% = 8h → eligible at 9h

    @BeforeEach void setUp() {
        fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        useCase = new EscalateTicketsUseCaseImpl(ticketRepo, historyRepo,
                new EscalationPolicy(), assignTx, metrics, fixedClock, 80);
    }

    private Ticket eligibleTicket(TicketStatus status) {
        return Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", "PC", status, null,
                created, created, deadline, null, null, null, null, null);
    }

    @Test @DisplayName("Eligible ABIERTO ticket gets escalated and priority raised")
    void eligible_escalated_priorityRaised() {
        Ticket ticket = eligibleTicket(TicketStatus.ABIERTO);
        when(ticketRepo.findOpenTickets()).thenReturn(List.of(ticket));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignTx.assignAndCommit(any(), isNull())).thenReturn(ticket);

        int count = useCase.escalate();

        assertThat(count).isEqualTo(1);
        assertThat(ticket.getPriority()).isEqualTo(TicketPriority.ALTA); // MEDIA → ALTA
        assertThat(ticket.getEscalatedAt()).isNotNull();
        verify(metrics).recordEscalated();
    }

    @Test @DisplayName("Overdue ticket also triggers slaBreached metric")
    void overdue_triggersBreachMetric() {
        Ticket ticket = eligibleTicket(TicketStatus.ABIERTO);
        // Make it overdue: deadline is in the past already (created 9h ago, 10h deadline → not yet)
        // Use a ticket already past deadline
        Ticket overdue = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", "PC", TicketStatus.ABIERTO, null,
                created, created, now.minusSeconds(1), // deadline already passed
                null, null, null, null, null);
        when(ticketRepo.findOpenTickets()).thenReturn(List.of(overdue));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignTx.assignAndCommit(any(), isNull())).thenReturn(overdue);

        useCase.escalate();

        verify(metrics).recordSlaBreached();
    }

    @Test @DisplayName("Already escalated ticket is skipped — idempotency")
    void alreadyEscalated_skipped() {
        Ticket ticket = Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", "PC", TicketStatus.ABIERTO, null,
                created, created, deadline, null, null,
                created, // escalatedAt set → already escalated
                null, null);
        when(ticketRepo.findOpenTickets()).thenReturn(List.of(ticket));

        int count = useCase.escalate();

        assertThat(count).isZero();
        verify(metrics, never()).recordEscalated();
    }

    @Test @DisplayName("ABIERTO escalated ticket retries assignment")
    void abiertoEscalated_retriesAssignment() {
        Ticket ticket = eligibleTicket(TicketStatus.ABIERTO);
        when(ticketRepo.findOpenTickets()).thenReturn(List.of(ticket));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignTx.assignAndCommit(any(), isNull())).thenReturn(ticket);

        useCase.escalate();

        verify(assignTx).assignAndCommit(ticket.getId(), null);
    }
}
