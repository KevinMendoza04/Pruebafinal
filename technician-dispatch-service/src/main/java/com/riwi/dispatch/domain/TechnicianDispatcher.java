package com.riwi.dispatch.domain;

import java.util.zip.CRC32;

/**
 * Pure domain class — no Spring annotations, no I/O.
 *
 * Deterministic algorithm (business-rules.md):
 *   seed        = CRC32( UTF-8("<ticketId>|<categoria>|<prioridad>") )
 *   techNum     = (seed % 20) + 1             → code TEC-%03d
 *   hours       = (seed / 20) % 13            → 0..12
 *   availability: 0-2 → ALTA | 3-8 → MEDIA | >8 → BAJA
 *
 * Using CRC32 from java.util.zip so the domain stays JDK-only.
 * Explicit UTF-8 encoding ensures identical results across locales.
 */
public class TechnicianDispatcher {

    /**
     * Compute a dispatch result for the given ticket context.
     * The same input will always produce the same output.
     */
    public DispatchResult dispatch(long ticketId, TicketCategory categoria, TicketPriority prioridad) {
        // Build the seed string exactly as specified
        String seedInput = ticketId + "|" + categoria.name() + "|" + prioridad.name();

        long seed = crc32(seedInput);

        int techNum   = (int) (seed % 20) + 1;           // 1..20
        int hours     = (int) ((seed / 20) % 13);         // 0..12

        String techCode    = String.format("TEC-%03d", techNum);
        Availability avail = classifyAvailability(hours);

        return new DispatchResult(techCode, hours, avail);
    }

    /**
     * Compute unsigned CRC32 of the UTF-8 encoding of the input string.
     * CRC32.getValue() returns a long in 0..4294967295 (unsigned 32-bit).
     */
    private long crc32(String input) {
        CRC32 crc = new CRC32();
        crc.update(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return crc.getValue();
    }

    /**
     * Classify estimated hours into availability tier.
     * Boundaries: 0-2 ALTA, 3-8 MEDIA, >8 BAJA
     */
    private Availability classifyAvailability(int hours) {
        if (hours <= 2) return Availability.ALTA;
        if (hours <= 8) return Availability.MEDIA;
        return Availability.BAJA;
    }
}
