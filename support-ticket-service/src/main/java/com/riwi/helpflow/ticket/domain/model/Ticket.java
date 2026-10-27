package com.riwi.helpflow.ticket.domain.model;

import com.riwi.helpflow.ticket.domain.exception.InvalidTicketTransitionException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich domain aggregate for a support ticket.
 * All state transitions go through this class; invariants are enforced here.
 * No Spring / Jakarta / Jackson / Lombok allowed.
 */
public class Ticket {

    private final UUID          id;
    private final UUID          requesterId;
    private String              title;
    private String              description;
    private TicketCategory      category;
    private TicketPriority      priority;
    private String              site;
    private String              affectedAsset;
    private TicketStatus        status;
    private UUID                assignedTechnicianId;  // null until assigned
    private final Instant       createdAt;
    private Instant             updatedAt;
    private Instant             slaDeadline;
    private Instant             firstRespondedAt;      // first EN_PROGRESO
    private Instant             resolvedAt;
    private Instant             escalatedAt;
    private Instant             slaBreachedAt;

    // Version field for optimistic locking (set/read by the persistence adapter)
    private Long version;

    private Ticket(UUID id, UUID requesterId, String title, String description,
                   TicketCategory category, TicketPriority priority, String site,
                   String affectedAsset, TicketStatus status, UUID assignedTechnicianId,
                   Instant createdAt, Instant updatedAt, Instant slaDeadline,
                   Instant firstRespondedAt, Instant resolvedAt,
                   Instant escalatedAt, Instant slaBreachedAt, Long version) {
        this.id                   = Objects.requireNonNull(id);
        this.requesterId          = Objects.requireNonNull(requesterId);
        this.title                = Objects.requireNonNull(title);
        this.description          = Objects.requireNonNull(description);
        this.category             = Objects.requireNonNull(category);
        this.priority             = Objects.requireNonNull(priority);
        this.site                 = site;
        this.affectedAsset        = affectedAsset;
        this.status               = Objects.requireNonNull(status);
        this.assignedTechnicianId = assignedTechnicianId;
        this.createdAt            = Objects.requireNonNull(createdAt);
        this.updatedAt            = updatedAt;
        this.slaDeadline          = slaDeadline;
        this.firstRespondedAt     = firstRespondedAt;
        this.resolvedAt           = resolvedAt;
        this.escalatedAt          = escalatedAt;
        this.slaBreachedAt        = slaBreachedAt;
        this.version              = version;
    }

    /**
     * Factory: creates a new ticket in ABIERTO status.
     * SLA deadline must be set separately by the use case after loading SlaPolicy.
     */
    public static Ticket create(UUID id, UUID requesterId, String title, String description,
                                TicketCategory category, TicketPriority priority,
                                String site, String affectedAsset, Instant now) {
        return new Ticket(id, requesterId, title, description, category, priority,
                site, affectedAsset, TicketStatus.ABIERTO, null,
                now, now, null, null, null, null, null, null);
    }

    /** Full reconstitution from persistence (all fields). */
    public static Ticket reconstitute(UUID id, UUID requesterId, String title, String description,
                                      TicketCategory category, TicketPriority priority,
                                      String site, String affectedAsset, TicketStatus status,
                                      UUID assignedTechnicianId, Instant createdAt, Instant updatedAt,
                                      Instant slaDeadline, Instant firstRespondedAt, Instant resolvedAt,
                                      Instant escalatedAt, Instant slaBreachedAt, Long version) {
        return new Ticket(id, requesterId, title, description, category, priority, site,
                affectedAsset, status, assignedTechnicianId, createdAt, updatedAt,
                slaDeadline, firstRespondedAt, resolvedAt, escalatedAt, slaBreachedAt, version);
    }

    // ── Business behaviour ───────────────────────────────────────────────────

    /** Sets the SLA deadline after the ticket is created. */
    public void setSlaDeadline(Instant deadline) {
        this.slaDeadline = Objects.requireNonNull(deadline);
    }

    /**
     * Applies a validated transition. Caller (TicketTransitionPolicy) must have
     * already verified that the transition is legal. This method updates state
     * and tracks timestamps.
     */
    public void applyTransition(TicketStatus newStatus, Instant now) {
        if (this.status.isTerminal()) {
            throw new InvalidTicketTransitionException(this.status, newStatus,
                    "El ticket está en estado terminal y no puede ser modificado");
        }
        TicketStatus previous = this.status;
        this.status    = newStatus;
        this.updatedAt = now;

        // Track first response timestamp (first move to EN_PROGRESO)
        if (newStatus == TicketStatus.EN_PROGRESO && firstRespondedAt == null) {
            this.firstRespondedAt = now;
        }
        // Track resolution timestamp
        if (newStatus == TicketStatus.RESUELTO) {
            this.resolvedAt = now;
        }
        // Reopen clears resolution timestamp
        if (previous == TicketStatus.RESUELTO && newStatus == TicketStatus.EN_PROGRESO) {
            this.resolvedAt = null;
        }
    }

    /**
     * Assigns the ticket to a technician (transitions ABIERTO -> ASIGNADO).
     * Called only by the assignment use case after eligibility checks.
     */
    public void assignTo(UUID technicianId, Instant now) {
        if (this.status != TicketStatus.ABIERTO && this.status != TicketStatus.ASIGNADO) {
            throw new InvalidTicketTransitionException(this.status, TicketStatus.ASIGNADO,
                    "Solo se puede asignar un ticket ABIERTO o ASIGNADO");
        }
        this.assignedTechnicianId = Objects.requireNonNull(technicianId);
        this.status    = TicketStatus.ASIGNADO;
        this.updatedAt = now;
    }

    /**
     * Escalates the ticket priority by one level and records escalation time.
     * Idempotent: does nothing if already escalated.
     *
     * WHY: escalation is a domain event; its logic belongs here, not in the scheduler.
     */
    public boolean escalate(Instant now) {
        if (escalatedAt != null) return false; // already escalated — idempotent
        this.priority    = priority.escalate();
        this.escalatedAt = now;
        this.updatedAt   = now;
        return true;
    }

    /** Marks SLA as breached (first time only — idempotent). */
    public boolean markSlaBreached(Instant now) {
        if (slaBreachedAt != null) return false;
        this.slaBreachedAt = now;
        this.updatedAt     = now;
        return true;
    }

    /** Returns true if the ticket is currently overdue. */
    public boolean isOverdue(Instant now) {
        return slaDeadline != null
                && now.isAfter(slaDeadline)
                && status.isOpen();
    }

    /** Returns true if the ticket is eligible for escalation at the given threshold. */
    public boolean isEligibleForEscalation(Instant now, int thresholdPercent) {
        if (escalatedAt != null) return false;
        if (!status.isOpen()) return false;
        if (slaDeadline == null) return false;

        long totalWindow  = slaDeadline.toEpochMilli() - createdAt.toEpochMilli();
        long elapsed      = now.toEpochMilli()         - createdAt.toEpochMilli();
        if (totalWindow <= 0) return true;
        return elapsed * 100L >= (long) thresholdPercent * totalWindow;
    }

    /** Updates editable fields (only while ABIERTO). */
    public void updateFields(String title, String description, String affectedAsset, Instant now) {
        if (this.status != TicketStatus.ABIERTO) {
            throw new com.riwi.helpflow.ticket.domain.exception.TicketNotEditableException(
                    "Solo se pueden editar tickets en estado ABIERTO");
        }
        this.title         = Objects.requireNonNull(title);
        this.description   = Objects.requireNonNull(description);
        this.affectedAsset = affectedAsset;
        this.updatedAt     = now;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public UUID          getId()                   { return id; }
    public UUID          getRequesterId()          { return requesterId; }
    public String        getTitle()                { return title; }
    public String        getDescription()          { return description; }
    public TicketCategory getCategory()            { return category; }
    public TicketPriority getPriority()            { return priority; }
    public String        getSite()                 { return site; }
    public String        getAffectedAsset()        { return affectedAsset; }
    public TicketStatus  getStatus()               { return status; }
    public UUID          getAssignedTechnicianId() { return assignedTechnicianId; }
    public Instant       getCreatedAt()            { return createdAt; }
    public Instant       getUpdatedAt()            { return updatedAt; }
    public Instant       getSlaDeadline()          { return slaDeadline; }
    public Instant       getFirstRespondedAt()     { return firstRespondedAt; }
    public Instant       getResolvedAt()           { return resolvedAt; }
    public Instant       getEscalatedAt()          { return escalatedAt; }
    public Instant       getSlaBreachedAt()        { return slaBreachedAt; }
    public Long          getVersion()              { return version; }
    public void          setVersion(Long v)        { this.version = v; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket t)) return false;
        return id.equals(t.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
