package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;

import java.util.UUID;

public interface CancelTicketUseCase {
    Ticket cancel(AuthenticatedActor actor, UUID ticketId, String observation);
}
