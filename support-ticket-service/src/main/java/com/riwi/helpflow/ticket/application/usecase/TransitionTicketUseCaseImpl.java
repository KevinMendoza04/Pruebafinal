package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.TransitionTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.TicketTransitionPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TransitionTicketUseCaseImpl implements TransitionTicketUseCase {

    private final TicketRepositoryPort       ticketRepo;
    private final TechnicianRepositoryPort   technicianRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final TicketTransitionPolicy     policy;
    private final TicketMetricsPort          metrics;
    private final Clock                      clock;

    public TransitionTicketUseCaseImpl(TicketRepositoryPort ticketRepo,
                                       TechnicianRepositoryPort technicianRepo,
                                       TicketHistoryRepositoryPort historyRepo,
                                       TicketTransitionPolicy policy,
                                       TicketMetricsPort metrics,
                                       Clock clock) {
        this.ticketRepo     = ticketRepo;
        this.technicianRepo = technicianRepo;
        this.historyRepo    = historyRepo;
        this.policy         = policy;
        this.metrics        = metrics;
        this.clock          = clock;
    }

    @Override
    @Transactional
    public Ticket transition(AuthenticatedActor actor, UUID ticketId, Command command) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket no encontrado: " + ticketId));

        boolean isOwner    = ticket.getRequesterId().equals(actor.userId());
        boolean isAssigned = false;

        if (actor.isTechnician()) {
            Optional<Technician> tech = technicianRepo.findByUserId(actor.userId());
            isAssigned = tech.map(t -> t.getId().equals(ticket.getAssignedTechnicianId()))
                            .orElse(false);
        }

        // Ownership check for USUARIO accessing non-own ticket
        if (actor.isUser() && !isOwner) {
            throw new ForbiddenOperationException(
                    "Acceso denegado: este ticket no le pertenece");
        }

        policy.validate(ticket, command.targetStatus(), actor.role(),
                isOwner, isAssigned, command.observation());

        Instant now = clock.instant();
        TicketStatus previousStatus = ticket.getStatus();
        ticket.applyTransition(command.targetStatus(), now);
        ticketRepo.save(ticket);

        // Record history
        historyRepo.save(TicketHistoryEntry.of(
                UUID.randomUUID(), ticket.getId(), HistoryEventType.STATUS_CHANGED,
                previousStatus, command.targetStatus(), actor.userId(),
                command.observation(), now));

        // Emit metrics for resolution and first response
        if (command.targetStatus() == TicketStatus.RESUELTO && ticket.getCreatedAt() != null) {
            metrics.recordResolution(Duration.between(ticket.getCreatedAt(), now));
        }
        if (command.targetStatus() == TicketStatus.EN_PROGRESO
                && ticket.getFirstRespondedAt() != null
                && ticket.getFirstRespondedAt().equals(now)) {
            metrics.recordFirstResponse(Duration.between(ticket.getCreatedAt(), now));
        }

        return ticket;
    }
}
