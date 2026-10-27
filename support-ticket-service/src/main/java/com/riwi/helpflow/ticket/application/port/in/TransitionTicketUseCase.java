package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;

import java.util.UUID;

public interface TransitionTicketUseCase {
    record Command(TicketStatus targetStatus, String observation) {}
    Ticket transition(AuthenticatedActor actor, UUID ticketId, Command command);
}
