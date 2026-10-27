package com.riwi.helpflow.ticket.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * SLA resolution deadline policy for a given priority.
 * Stores the number of hours allowed to resolve a ticket.
 * Cross-validation (CRITICA < ALTA < MEDIA < BAJA) is done in the use case.
 */
public class SlaPolicy {

    private final UUID id;
    private TicketPriority priority;
    private int resolutionHours;

    private SlaPolicy(UUID id, TicketPriority priority, int resolutionHours) {
        this.id = Objects.requireNonNull(id);
        this.priority = Objects.requireNonNull(priority);
        if (resolutionHours <= 0) throw new IllegalArgumentException("resolutionHours must be > 0");
        this.resolutionHours = resolutionHours;
    }

    public static SlaPolicy of(UUID id, TicketPriority priority, int resolutionHours) {
        return new SlaPolicy(id, priority, resolutionHours);
    }

    public void updateHours(int hours) {
        if (hours <= 0) throw new IllegalArgumentException("resolutionHours must be > 0");
        this.resolutionHours = hours;
    }

    public UUID           getId()              { return id; }
    public TicketPriority getPriority()        { return priority; }
    public int            getResolutionHours() { return resolutionHours; }
}
