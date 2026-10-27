package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.GetTicketsUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GetTicketsUseCaseImpl implements GetTicketsUseCase {

    private final TicketRepositoryPort        ticketRepo;
    private final TechnicianRepositoryPort    technicianRepo;
    private final TicketHistoryRepositoryPort historyRepo;

    public GetTicketsUseCaseImpl(TicketRepositoryPort ticketRepo,
                                 TechnicianRepositoryPort technicianRepo,
                                 TicketHistoryRepositoryPort historyRepo) {
        this.ticketRepo     = ticketRepo;
        this.technicianRepo = technicianRepo;
        this.historyRepo    = historyRepo;
    }

    @Override
    public List<Ticket> listTickets(AuthenticatedActor actor, int page, int size,
                                    TicketStatus status, TicketPriority priority,
                                    TicketCategory category) {
        if (actor.isAdmin()) {
            return ticketRepo.findAll(page, size, status, priority, category);
        }
        if (actor.isTechnician()) {
            UUID techId = technicianRepo.findByUserId(actor.userId())
                    .map(Technician::getId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Perfil de técnico no encontrado"));
            return ticketRepo.findByTechnicianId(techId, page, size);
        }
        // ROLE_USUARIO — own tickets only
        return ticketRepo.findByRequesterId(actor.userId(), page, size);
    }

    @Override
    public Ticket getTicketById(AuthenticatedActor actor, UUID id) {
        Ticket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket no encontrado: " + id));
        checkAccess(actor, ticket);
        return ticket;
    }

    @Override
    public List<TicketHistoryEntry> getHistory(AuthenticatedActor actor, UUID ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket no encontrado: " + ticketId));
        checkAccess(actor, ticket);
        return historyRepo.findByTicketId(ticketId);
    }

    private void checkAccess(AuthenticatedActor actor, Ticket ticket) {
        if (actor.isAdmin()) return;
        if (actor.isUser() && ticket.getRequesterId().equals(actor.userId())) return;
        if (actor.isTechnician()) {
            Optional<Technician> tech = technicianRepo.findByUserId(actor.userId());
            if (tech.isPresent() && tech.get().getId().equals(ticket.getAssignedTechnicianId()))
                return;
        }
        // Consistent 403 regardless of whether ticket exists — no information leakage
        throw new ForbiddenOperationException(
                "Acceso denegado: no tiene permisos para ver este ticket");
    }
}
