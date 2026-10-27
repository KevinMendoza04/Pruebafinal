package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.ReassignTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.TechnicianSelectionPolicy;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class ReassignTicketUseCaseImpl implements ReassignTicketUseCase {

    private final TicketRepositoryPort        ticketRepo;
    private final TechnicianRepositoryPort    technicianRepo;
    private final AssignmentRepositoryPort    assignmentRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final TechnicianSelectionPolicy   selectionPolicy;
    private final Clock                       clock;

    public ReassignTicketUseCaseImpl(TicketRepositoryPort ticketRepo,
                                     TechnicianRepositoryPort technicianRepo,
                                     AssignmentRepositoryPort assignmentRepo,
                                     TicketHistoryRepositoryPort historyRepo,
                                     TechnicianSelectionPolicy selectionPolicy,
                                     Clock clock) {
        this.ticketRepo      = ticketRepo;
        this.technicianRepo  = technicianRepo;
        this.assignmentRepo  = assignmentRepo;
        this.historyRepo     = historyRepo;
        this.selectionPolicy = selectionPolicy;
        this.clock           = clock;
    }

    @Override
    @Transactional
    public Ticket reassign(AuthenticatedActor actor, UUID ticketId, UUID newTechnicianId) {
        if (!actor.isAdmin()) {
            throw new ForbiddenOperationException(
                    "Solo los administradores pueden reasignar tickets");
        }
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket no encontrado: " + ticketId));

        // Allowed states for reassignment
        if (ticket.getStatus() != TicketStatus.ABIERTO
                && ticket.getStatus() != TicketStatus.ASIGNADO
                && ticket.getStatus() != TicketStatus.EN_PROGRESO) {
            throw new ForbiddenOperationException(
                    "No se puede reasignar un ticket en estado: " + ticket.getStatus());
        }

        // Lock and validate new technician
        Technician newTech = technicianRepo.lockForUpdate(newTechnicianId);
        if (newTech == null) {
            throw new ResourceNotFoundException(
                    "Técnico no encontrado: " + newTechnicianId);
        }

        // Hard eligibility rules (no SLA rule for reassignment)
        java.util.List<Technician> candidates = selectionPolicy.getEligibleCandidates(
                java.util.List.of(newTech), ticket.getCategory(),
                ticket.getPriority(), ticket.getSite());
        if (candidates.isEmpty()) {
            throw new ForbiddenOperationException(
                    "El técnico no cumple los criterios de elegibilidad para este ticket");
        }

        Instant now = clock.instant();

        // Close previous assignment if exists
        assignmentRepo.findActiveByTicketId(ticketId).ifPresent(prev -> {
            prev.close(now);
            assignmentRepo.save(prev);
        });

        // Create new assignment
        Assignment newAssignment = Assignment.create(
                UUID.randomUUID(), ticketId, newTechnicianId, now);
        assignmentRepo.save(newAssignment);

        ticket.assignTo(newTechnicianId, now);
        ticketRepo.save(ticket);

        historyRepo.save(TicketHistoryEntry.of(
                UUID.randomUUID(), ticketId, HistoryEventType.REASSIGNED,
                null, ticket.getStatus(), actor.userId(),
                "Reasignado a técnico " + newTech.getCode(), now));

        return ticket;
    }
}
