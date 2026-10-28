package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.application.usecase.AssignTicketTxPort;
import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.application.usecase.CreateTicketTxPort;
import com.riwi.helpflow.ticket.domain.exception.InactiveUserException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.SlaCalculator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TX-1: validates requester, creates ticket ABIERTO + SLA deadline + CREATED history.
 * This is a separate Spring bean to avoid self-invocation proxy bypass.
 */
@Component
public class CreateTicketTxAdapter implements CreateTicketTxPort {

    private final TicketRepositoryPort        ticketRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final UserRepositoryPort           userRepo;
    private final SlaPolicyRepositoryPort     slaPolicyRepo;
    private final SlaCalculator               slaCalculator;
    private final Clock                        clock;

    public CreateTicketTxAdapter(TicketRepositoryPort ticketRepo,
                                  TicketHistoryRepositoryPort historyRepo,
                                  UserRepositoryPort userRepo,
                                  SlaPolicyRepositoryPort slaPolicyRepo,
                                  SlaCalculator slaCalculator,
                                  Clock clock) {
        this.ticketRepo    = ticketRepo;
        this.historyRepo   = historyRepo;
        this.userRepo       = userRepo;
        this.slaPolicyRepo  = slaPolicyRepo;
        this.slaCalculator  = slaCalculator;
        this.clock          = clock;
    }

    @Override
    @Transactional
    public Ticket createAndCommit(AuthenticatedActor actor, CreateTicketUseCase.Command cmd) {
        // Validate requester exists and is ACTIVO
        User requester = userRepo.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario solicitante no encontrado"));
        if (!requester.isActive()) {
            throw new InactiveUserException(
                    "El usuario solicitante no está activo");
        }

        Instant now = clock.instant();

        // Create ticket ABIERTO
        Ticket ticket = Ticket.create(UUID.randomUUID(), actor.userId(),
                cmd.title(), cmd.description(), cmd.category(), cmd.priority(),
                cmd.site(), cmd.affectedAsset(), now);

        // Compute SLA deadline
        List<SlaPolicy> policies = slaPolicyRepo.findAll();
        Instant deadline = slaCalculator.computeDeadline(now, cmd.priority(), policies);
        ticket.setSlaDeadline(deadline);

        ticket = ticketRepo.save(ticket);

        // CREATED history
        historyRepo.save(TicketHistoryEntry.of(UUID.randomUUID(), ticket.getId(),
                HistoryEventType.CREATED, null, TicketStatus.ABIERTO,
                actor.userId(), "Ticket creado", now));

        return ticket;
    }
}
