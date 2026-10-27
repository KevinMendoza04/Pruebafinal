package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

public interface CreateTicketUseCase {
    record Command(String title, String description, TicketCategory category,
                   TicketPriority priority, String site, String affectedAsset) {}
    Ticket create(AuthenticatedActor actor, Command command);
}
