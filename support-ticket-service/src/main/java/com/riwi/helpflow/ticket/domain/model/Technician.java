package com.riwi.helpflow.ticket.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Technician profile linked 1:1 to a User with ROLE_TECNICO.
 * Contains specialties (set of TicketCategory), site, support profile text,
 * status, and max concurrent active tickets.
 */
public class Technician {

    private final UUID id;
    private final UUID userId;
    private String code;           // TEC-001 .. TEC-020 (or any unique code)
    private Set<TicketCategory> specialties;
    private String site;
    private String supportProfile;
    private TechnicianStatus status;
    private int maxActiveTickets;  // default 5 per business rules

    /** Current count of ASIGNADO + EN_PROGRESO tickets for this technician.
     *  NOT persisted here; injected by the query layer when needed. */
    private int activeTicketCount;

    private Technician(UUID id, UUID userId, String code,
                       Set<TicketCategory> specialties, String site,
                       String supportProfile, TechnicianStatus status,
                       int maxActiveTickets) {
        this.id             = Objects.requireNonNull(id);
        this.userId         = Objects.requireNonNull(userId);
        this.code           = Objects.requireNonNull(code);
        this.specialties    = specialties == null
                ? EnumSet.noneOf(TicketCategory.class)
                : EnumSet.copyOf(specialties.isEmpty()
                        ? EnumSet.noneOf(TicketCategory.class)
                        : specialties);
        this.site           = site;
        this.supportProfile = supportProfile;
        this.status         = Objects.requireNonNull(status);
        this.maxActiveTickets = maxActiveTickets > 0 ? maxActiveTickets : 5;
    }

    public static Technician create(UUID id, UUID userId, String code,
                                    Set<TicketCategory> specialties, String site,
                                    String supportProfile, TechnicianStatus status,
                                    int maxActiveTickets) {
        return new Technician(id, userId, code, specialties, site,
                supportProfile, status, maxActiveTickets);
    }

    /**
     * Returns true if this technician is eligible to handle a ticket with the given category.
     * OTRO is handled by everyone (wildcard).
     */
    public boolean isCompatibleWith(TicketCategory category) {
        return category == TicketCategory.OTRO || specialties.contains(category);
    }

    /** Returns true if this technician can accept one more ticket. */
    public boolean hasCapacity() {
        return activeTicketCount < maxActiveTickets;
    }

    public boolean isActive() { return TechnicianStatus.ACTIVO == this.status; }

    public void deactivate() { this.status = TechnicianStatus.INACTIVO; }
    public void activate()   { this.status = TechnicianStatus.ACTIVO; }

    // ── Setters used only by the persistence adapter ──────────────────────
    public void setActiveTicketCount(int count) { this.activeTicketCount = count; }

    // ── Getters ──────────────────────────────────────────────────────────
    public UUID             getId()               { return id; }
    public UUID             getUserId()           { return userId; }
    public String           getCode()             { return code; }
    public Set<TicketCategory> getSpecialties()   { return Collections.unmodifiableSet(specialties); }
    public String           getSite()             { return site; }
    public String           getSupportProfile()   { return supportProfile; }
    public TechnicianStatus getStatus()           { return status; }
    public int              getMaxActiveTickets() { return maxActiveTickets; }
    public int              getActiveTicketCount(){ return activeTicketCount; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Technician t)) return false;
        return id.equals(t.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
