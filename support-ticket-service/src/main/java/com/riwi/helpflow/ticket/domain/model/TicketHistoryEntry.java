package com.riwi.helpflow.ticket.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit entry for every ticket event.
 * actorId may be null when the actor is the SYSTEM (e.g. auto-assignment).
 */
public class TicketHistoryEntry {

    private final UUID             id;
    private final UUID             ticketId;
    private final HistoryEventType eventType;
    private final TicketStatus     fromStatus;
    private final TicketStatus     toStatus;
    private final UUID             actorId;    // null = SYSTEM
    private final String           observation;
    private final Instant          createdAt;

    private TicketHistoryEntry(UUID id, UUID ticketId, HistoryEventType eventType,
                               TicketStatus fromStatus, TicketStatus toStatus,
                               UUID actorId, String observation, Instant createdAt) {
        this.id          = Objects.requireNonNull(id);
        this.ticketId    = Objects.requireNonNull(ticketId);
        this.eventType   = Objects.requireNonNull(eventType);
        this.fromStatus  = fromStatus;
        this.toStatus    = toStatus;
        this.actorId     = actorId;
        this.observation = observation;
        this.createdAt   = Objects.requireNonNull(createdAt);
    }

    public static TicketHistoryEntry of(UUID id, UUID ticketId, HistoryEventType eventType,
                                        TicketStatus fromStatus, TicketStatus toStatus,
                                        UUID actorId, String observation, Instant createdAt) {
        return new TicketHistoryEntry(id, ticketId, eventType, fromStatus, toStatus,
                actorId, observation, createdAt);
    }

    public UUID             getId()          { return id; }
    public UUID             getTicketId()    { return ticketId; }
    public HistoryEventType getEventType()   { return eventType; }
    public TicketStatus     getFromStatus()  { return fromStatus; }
    public TicketStatus     getToStatus()    { return toStatus; }
    public UUID             getActorId()     { return actorId; }
    public String           getObservation() { return observation; }
    public Instant          getCreatedAt()   { return createdAt; }
}
