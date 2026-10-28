package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.CancelTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.TicketHistoryRepositoryPort;
import com.riwi.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.HistoryEventType;
import com.riwi.helpflow.ticket.domain.model.Role;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketHistoryEntry;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import com.riwi.helpflow.ticket.domain.service.TicketTransitionPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class CancelTicketUseCaseImpl implements CancelTicketUseCase {

    private final TicketRepositoryPort        ticketRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final TicketTransitionPolicy     policy;
    private final Clock                      clock;

    public CancelTicketUseCaseImpl(TicketRepositoryPort ticketRepo,
                                   TicketHistoryRepositoryPort historyRepo,
                                   TicketTransitionPolicy policy, Clock clock) {
        this.ticketRepo = ticketRepo;
        this.historyRepo = historyRepo;
        this.policy     = policy;
        this.clock       = clock;
    }

    @Override
    @Transactional
    public Ticket cancel(AuthenticatedActor actor, UUID ticketId, String observation) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado: " + ticketId));

        boolean isOwner = ticket.getRequesterId().equals(actor.userId());
        boolean isAssigned = false;

        policy.validate(ticket, TicketStatus.CANCELADO, actor.role(),
                isOwner, isAssigned, observation);

        TicketStatus previous = ticket.getStatus();
        ticket.applyTransition(TicketStatus.CANCELADO, clock.instant());
        ticketRepo.save(ticket);

        historyRepo.save(TicketHistoryEntry.of(UUID.randomUUID(), ticket.getId(),
                HistoryEventType.STATUS_CHANGED, previous, TicketStatus.CANCELADO,
                actor.userId(), observation, clock.instant()));

        return ticket;
    }
}
