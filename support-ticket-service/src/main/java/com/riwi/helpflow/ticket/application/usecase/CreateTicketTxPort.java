package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.riwi.helpflow.ticket.domain.model.Ticket;

/**
 * Internal transactional collaborator for TX-1 of ticket creation.
 * Defined in the application layer so CreateTicketUseCaseImpl can depend on it
 * without touching infrastructure.
 */
public interface CreateTicketTxPort {
    Ticket createAndCommit(AuthenticatedActor actor, CreateTicketUseCase.Command command);
}
