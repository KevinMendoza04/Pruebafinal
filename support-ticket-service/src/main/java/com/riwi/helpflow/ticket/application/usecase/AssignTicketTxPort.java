package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.riwi.helpflow.ticket.domain.model.Ticket;

import java.util.UUID;

/**
 * Internal transactional collaborator for TX-2 of ticket creation (the assignment step).
 */
public interface AssignTicketTxPort {
    Ticket assignAndCommit(UUID ticketId, TechnicianDispatchPort.DispatchSuggestion suggestion);
}
