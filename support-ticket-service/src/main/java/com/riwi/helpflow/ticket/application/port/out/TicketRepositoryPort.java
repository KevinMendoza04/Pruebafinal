package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepositoryPort {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(UUID id);
    List<Ticket> findByRequesterId(UUID requesterId, int page, int size);
    List<Ticket> findByTechnicianId(UUID technicianId, int page, int size);
    List<Ticket> findAll(int page, int size,
                         TicketStatus status, TicketPriority priority, TicketCategory category);
    /** Returns all open tickets for the escalation scan. */
    List<Ticket> findOpenTickets();
    /** Count of currently overdue tickets (for gauge metric). */
    long countOverdue(Instant now);
}
