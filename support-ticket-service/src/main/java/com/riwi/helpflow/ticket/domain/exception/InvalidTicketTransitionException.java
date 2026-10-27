package com.riwi.helpflow.ticket.domain.exception;

import com.riwi.helpflow.ticket.domain.model.TicketStatus;

public class InvalidTicketTransitionException extends DomainException {
    public InvalidTicketTransitionException(TicketStatus from, TicketStatus to, String reason) {
        super("Transición inválida de " + from + " a " + to + ": " + reason);
    }
}
