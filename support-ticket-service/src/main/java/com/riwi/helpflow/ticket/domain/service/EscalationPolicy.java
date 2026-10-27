package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.model.Ticket;

import java.time.Instant;
import java.util.List;

/**
 * Domain policy for ticket escalation logic.
 * Determines which tickets are eligible for escalation given a threshold percentage.
 */
public class EscalationPolicy {

    /**
     * Filters the supplied tickets and returns only those eligible for escalation.
     *
     * Eligible = open status + not yet escalated + elapsed time >= thresholdPercent of SLA window.
     *
     * @param tickets          all open tickets
     * @param now              current time from injected Clock
     * @param thresholdPercent e.g. 80 → escalate when 80% of SLA window has elapsed
     * @return tickets that should be escalated now
     */
    public List<Ticket> findEligible(List<Ticket> tickets, Instant now, int thresholdPercent) {
        return tickets.stream()
                .filter(t -> t.isEligibleForEscalation(now, thresholdPercent))
                .toList();
    }
}
