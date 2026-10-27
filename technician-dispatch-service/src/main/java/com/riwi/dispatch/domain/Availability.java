package com.riwi.dispatch.domain;

/**
 * Technician availability derived from the estimated hours.
 * Boundaries per business rules:
 *   0–2  hours  → ALTA
 *   3–8  hours  → MEDIA
 *   >8   hours  → BAJA
 */
public enum Availability {
    ALTA, MEDIA, BAJA
}
