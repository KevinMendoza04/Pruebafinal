package com.riwi.dispatch.domain;

/**
 * Ticket priorities as defined in the HelpFlow business rules.
 * Values are Spanish to match the exact JSON contract (no accent on CRITICA).
 */
public enum TicketPriority {
    BAJA, MEDIA, ALTA, CRITICA
}
