package com.riwi.helpflow.ticket.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Records which technician is (or was) assigned to a ticket.
 * unassignedAt is null while the assignment is still active.
 */
public class Assignment {

    private final UUID    id;
    private final UUID    ticketId;
    private final UUID    technicianId;
    private final Instant assignedAt;
    private Instant       unassignedAt;

    private Assignment(UUID id, UUID ticketId, UUID technicianId,
                       Instant assignedAt, Instant unassignedAt) {
        this.id           = Objects.requireNonNull(id);
        this.ticketId     = Objects.requireNonNull(ticketId);
        this.technicianId = Objects.requireNonNull(technicianId);
        this.assignedAt   = Objects.requireNonNull(assignedAt);
        this.unassignedAt = unassignedAt;
    }

    public static Assignment create(UUID id, UUID ticketId, UUID technicianId, Instant assignedAt) {
        return new Assignment(id, ticketId, technicianId, assignedAt, null);
    }

    /** Marks this assignment as closed (during reassignment). */
    public void close(Instant at) {
        if (this.unassignedAt != null) throw new IllegalStateException("Assignment already closed");
        this.unassignedAt = Objects.requireNonNull(at);
    }

    public boolean isActive() { return unassignedAt == null; }

    public UUID    getId()           { return id; }
    public UUID    getTicketId()     { return ticketId; }
    public UUID    getTechnicianId() { return technicianId; }
    public Instant getAssignedAt()   { return assignedAt; }
    public Instant getUnassignedAt() { return unassignedAt; }
}
