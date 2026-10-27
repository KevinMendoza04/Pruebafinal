package com.riwi.helpflow.ticket.application.port.in;

public interface EscalateTicketsUseCase {
    /** Runs the escalation scan. Returns the number of tickets escalated. */
    int escalate();
}
