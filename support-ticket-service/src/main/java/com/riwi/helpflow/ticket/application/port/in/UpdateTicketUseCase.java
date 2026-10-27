package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;

import java.util.UUID;

public interface UpdateTicketUseCase {
    record Command(String title, String description, String affectedAsset) {}
    Ticket update(AuthenticatedActor actor, UUID ticketId, Command command);
}
