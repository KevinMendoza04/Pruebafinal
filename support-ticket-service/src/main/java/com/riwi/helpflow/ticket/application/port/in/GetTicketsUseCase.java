package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketHistoryEntry;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;

import java.util.List;
import java.util.UUID;

public interface GetTicketsUseCase {
    List<Ticket> listTickets(AuthenticatedActor actor, int page, int size,
                             TicketStatus status, TicketPriority priority, TicketCategory category);
    Ticket getTicketById(AuthenticatedActor actor, UUID id);
    List<TicketHistoryEntry> getHistory(AuthenticatedActor actor, UUID ticketId);
}
