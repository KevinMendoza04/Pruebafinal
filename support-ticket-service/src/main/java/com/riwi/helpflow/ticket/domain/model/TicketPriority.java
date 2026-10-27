package com.riwi.helpflow.ticket.domain.model;

/** Priority levels. No accent on CRITICA per business-rules.md. */
public enum TicketPriority {
    BAJA, MEDIA, ALTA, CRITICA;

    /** Returns the next higher priority, or the same if already CRITICA. */
    public TicketPriority escalate() {
        return switch (this) {
            case BAJA    -> MEDIA;
            case MEDIA   -> ALTA;
            case ALTA    -> CRITICA;
            case CRITICA -> CRITICA;
        };
    }
}
