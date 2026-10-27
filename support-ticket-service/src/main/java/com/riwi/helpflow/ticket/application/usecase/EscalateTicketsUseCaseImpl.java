package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.EscalateTicketsUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.EscalationPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Runs the escalation scan.
 * For each eligible ticket (per EscalationPolicy):
 *   1. Raise priority by one level, set escalatedAt — idempotent.
 *   2. Add ESCALATED history entry.
 *   3. If ticket is still ABIERTO, trigger re-assignment (via AssignTicketTxPort).
 *   4. Set slaBreachedAt if currently overdue (first time).
 * All in one transaction per ticket (business-rules.md: "inside ONE transaction").
 */
public class EscalateTicketsUseCaseImpl implements EscalateTicketsUseCase {

    private final TicketRepositoryPort        ticketRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final EscalationPolicy            policy;
    private final AssignTicketTxPort          assignTx;
    private final TicketMetricsPort           metrics;
    private final Clock                       clock;
    private final int                         thresholdPercent;

    public EscalateTicketsUseCaseImpl(TicketRepositoryPort ticketRepo,
                                      TicketHistoryRepositoryPort historyRepo,
                                      EscalationPolicy policy,
                                      AssignTicketTxPort assignTx,
                                      TicketMetricsPort metrics,
                                      Clock clock,
                                      int thresholdPercent) {
        this.ticketRepo        = ticketRepo;
        this.historyRepo       = historyRepo;
        this.policy            = policy;
        this.assignTx          = assignTx;
        this.metrics           = metrics;
        this.clock             = clock;
        this.thresholdPercent  = thresholdPercent;
    }

    @Override
    @Transactional
    public int escalate() {
        Instant now   = clock.instant();
        List<Ticket> openTickets  = ticketRepo.findOpenTickets();
        List<Ticket> eligible     = policy.findEligible(openTickets, now, thresholdPercent);

        for (Ticket ticket : eligible) {
            processEscalation(ticket, now);
        }
        return eligible.size();
    }

    private void processEscalation(Ticket ticket, Instant now) {
        boolean escalated = ticket.escalate(now); // idempotent
        if (!escalated) return;

        // Mark SLA breach if overdue (idempotent)
        if (ticket.isOverdue(now)) {
            boolean breached = ticket.markSlaBreached(now);
            if (breached) metrics.recordSlaBreached();
        }

        ticketRepo.save(ticket);

        historyRepo.save(TicketHistoryEntry.of(
                UUID.randomUUID(), ticket.getId(), HistoryEventType.ESCALATED,
                null, ticket.getStatus(), null, // SYSTEM actor
                "Escalado automáticamente al " + thresholdPercent + "% del SLA", now));

        metrics.recordEscalated();

        // If still ABIERTO (unassigned), retry assignment
        if (ticket.getStatus() == TicketStatus.ABIERTO) {
            assignTx.assignAndCommit(ticket.getId(), null);
        }
    }
}
