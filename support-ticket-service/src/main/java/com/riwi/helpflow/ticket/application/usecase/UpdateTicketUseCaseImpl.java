package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.UpdateTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class UpdateTicketUseCaseImpl implements UpdateTicketUseCase {

    private final TicketRepositoryPort ticketRepo;
    private final Clock                clock;

    public UpdateTicketUseCaseImpl(TicketRepositoryPort ticketRepo, Clock clock) {
        this.ticketRepo = ticketRepo;
        this.clock      = clock;
    }

    @Override
    @Transactional
    public Ticket update(AuthenticatedActor actor, UUID ticketId, Command command) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket no encontrado: " + ticketId));

        boolean isOwner = ticket.getRequesterId().equals(actor.userId());
        if (!actor.isAdmin() && !isOwner) {
            throw new ForbiddenOperationException(
                    "Solo el propietario o un administrador puede editar este ticket");
        }

        ticket.updateFields(command.title(), command.description(),
                command.affectedAsset(), clock.instant());
        return ticketRepo.save(ticket);
    }
}
