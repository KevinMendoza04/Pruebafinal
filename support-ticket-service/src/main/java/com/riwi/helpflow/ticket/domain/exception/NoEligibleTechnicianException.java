package com.riwi.helpflow.ticket.domain.exception;

public class NoEligibleTechnicianException extends DomainException {
    public NoEligibleTechnicianException(String reason) {
        super("No hay técnico elegible: " + reason);
    }
}
