package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.DispatchServiceException;
import com.riwi.helpflow.ticket.domain.exception.InactiveUserException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.SlaCalculator;
import com.riwi.helpflow.ticket.domain.service.TechnicianSelectionPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the create-ticket flow.
 *
 * TX-1 / remote call / TX-2 design (business-rules.md):
 *   TX-1:  validate requester, create ticket ABIERTO + SLA deadline + CREATED history → commit.
 *   OUTSIDE TX: call TechnicianDispatchPort (may throw DispatchServiceException).
 *   TX-2:  load ticket, select technician, lock technician row, assign + ASSIGNED history → commit.
 *
 * WHY two transactions: if dispatch call blocks or fails, TX-1 already committed and the
 * ticket is safe. If TX-2 fails the ticket stays ABIERTO (not lost). The remote call must
 * never hold a database connection open.
 *
 * NOTE: @Transactional is the ONE tolerated Spring import in application per structure.md ADR.
 * TX-1 and TX-2 are on separate collaborator beans (CreateTicketTx and AssignTicketTx)
 * to avoid self-invocation proxy bypass.
 */
public class CreateTicketUseCaseImpl implements CreateTicketUseCase {

    private final CreateTicketTxPort          createTx;
    private final AssignTicketTxPort          assignTx;
    private final TechnicianDispatchPort      dispatch;
    private final TicketMetricsPort           metrics;

    public CreateTicketUseCaseImpl(CreateTicketTxPort createTx,
                                   AssignTicketTxPort assignTx,
                                   TechnicianDispatchPort dispatch,
                                   TicketMetricsPort metrics) {
        this.createTx = createTx;
        this.assignTx = assignTx;
        this.dispatch = dispatch;
        this.metrics  = metrics;
    }

    @Override
    public Ticket create(AuthenticatedActor actor, Command command) {
        // TX-1: create ticket and commit
        Ticket ticket = createTx.createAndCommit(actor, command);

        metrics.ticketCreated(ticket.getCategory(), ticket.getPriority());

        // Outside TX: call dispatch service (may fail — ticket is already safe)
        TechnicianDispatchPort.DispatchSuggestion suggestion = null;
        try {
            suggestion = dispatch.suggest(
                    ticket.getId().getMostSignificantBits() & Long.MAX_VALUE, // stable long id
                    ticket.getCategory(),
                    ticket.getPriority());
        } catch (RuntimeException ex) {
            metrics.recordDispatchFailure();
            // TX-2 will handle no-suggestion path (ticket stays ABIERTO)
        }

        // TX-2: try to assign; safe even if suggestion is null
        return assignTx.assignAndCommit(ticket.getId(), suggestion);
    }
}
