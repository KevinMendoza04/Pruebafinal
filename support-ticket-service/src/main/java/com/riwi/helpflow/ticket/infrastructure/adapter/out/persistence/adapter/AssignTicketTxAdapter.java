package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.application.usecase.AssignTicketTxPort;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.TechnicianSelectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TX-2: evaluates dispatch suggestion, selects technician, assigns ticket.
 * Separate bean from CreateTicketUseCaseImpl to avoid self-invocation proxy bypass.
 *
 * Flow:
 *   1. Re-load ticket (may have been modified between TX-1 and TX-2).
 *   2. If suggestion provided → check eligibility (hard rules + SLA rule).
 *   3. If suggestion rejected/null → fallback ordering among eligible candidates.
 *   4. Lock chosen technician, re-count active tickets, assign.
 *   5. No eligible → ticket stays ABIERTO, history ASSIGNMENT_FAILED.
 */
@Component
public class AssignTicketTxAdapter implements AssignTicketTxPort {

    private static final Logger log = LoggerFactory.getLogger(AssignTicketTxAdapter.class);

    private final TicketRepositoryPort        ticketRepo;
    private final TechnicianRepositoryPort    technicianRepo;
    private final AssignmentRepositoryPort     assignmentRepo;
    private final TicketHistoryRepositoryPort historyRepo;
    private final TechnicianSelectionPolicy    selectionPolicy;
    private final Clock                        clock;

    public AssignTicketTxAdapter(TicketRepositoryPort ticketRepo,
                                  TechnicianRepositoryPort technicianRepo,
                                  AssignmentRepositoryPort assignmentRepo,
                                  TicketHistoryRepositoryPort historyRepo,
                                  TechnicianSelectionPolicy selectionPolicy,
                                  Clock clock) {
        this.ticketRepo      = ticketRepo;
        this.technicianRepo   = technicianRepo;
        this.assignmentRepo   = assignmentRepo;
        this.historyRepo      = historyRepo;
        this.selectionPolicy  = selectionPolicy;
        this.clock            = clock;
    }

    @Override
    @Transactional
    public Ticket assignAndCommit(UUID ticketId, TechnicianDispatchPort.DispatchSuggestion suggestion) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado: " + ticketId));

        Instant now = clock.instant();
        Technician chosen = null;

        // ── Try dispatch recommendation first ───────────────────────────────
        if (suggestion != null) {
            Optional<Technician> recommended = technicianRepo.findByCode(suggestion.technicianCode());
            if (recommended.isPresent()) {
                Technician tech = recommended.get();
                // Populate active count for eligibility check
                tech = technicianRepo.findById(tech.getId()).orElse(tech);
                boolean eligible = selectionPolicy.isRecommendedEligible(
                        tech, ticket.getCategory(), ticket.getPriority(),
                        ticket.getSite(), ticket.getSlaDeadline(), now,
                        suggestion.estimatedHours());
                if (eligible) {
                    // Lock and re-count under pessimistic lock
                    chosen = technicianRepo.lockForUpdate(tech.getId());
                }
            }
        }

        // ── Fallback: among eligible candidates ─────────────────────────────
        if (chosen == null) {
            List<Technician> candidates = technicianRepo.findActiveByCategory(ticket.getCategory());
            List<Technician> eligible = selectionPolicy.getEligibleCandidates(
                    candidates, ticket.getCategory(), ticket.getPriority(), ticket.getSite());
            Optional<Technician> fallback = selectionPolicy.selectFallback(eligible, ticket.getSite());
            if (fallback.isPresent()) {
                // Lock and re-count
                chosen = technicianRepo.lockForUpdate(fallback.get().getId());
            }
        }

        // ── Assign or fail ──────────────────────────────────────────────────
        if (chosen != null && chosen.hasCapacity()) {
            // Success: create assignment, ticket → ASIGNADO
            Assignment assignment = Assignment.create(UUID.randomUUID(),
                    ticket.getId(), chosen.getId(), now);
            assignmentRepo.save(assignment);

            ticket.assignTo(chosen.getId(), now);
            ticketRepo.save(ticket);

            historyRepo.save(TicketHistoryEntry.of(UUID.randomUUID(), ticket.getId(),
                    HistoryEventType.ASSIGNED, TicketStatus.ABIERTO, TicketStatus.ASIGNADO,
                    null, "Asignado a " + chosen.getCode(), now));

            log.info("Ticket {} assigned to technician {}", ticket.getId(), chosen.getCode());
        } else {
            // No eligible technician: ticket stays ABIERTO
            historyRepo.save(TicketHistoryEntry.of(UUID.randomUUID(), ticket.getId(),
                    HistoryEventType.ASSIGNMENT_FAILED, TicketStatus.ABIERTO, TicketStatus.ABIERTO,
                    null, "No hay técnico disponible o elegible", now));

            log.info("Ticket {} stays ABIERTO — no eligible technician", ticket.getId());
        }

        return ticket;
    }
}
