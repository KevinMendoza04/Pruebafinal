package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.model.Technician;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Domain policy that evaluates technician eligibility and selects the best candidate.
 *
 * Hard eligibility rules (must all pass):
 *   1. Technician is ACTIVO
 *   2. Specialty compatible (contains category, or category is OTRO)
 *   3. Active ticket count < maxActiveTickets
 *   4. Site rule: ALTA/CRITICA → same site required; BAJA/MEDIA → same site preferred (soft)
 *
 * SLA rule (dispatch recommended technician only):
 *   5. tiempoEstimadoHoras <= hours remaining to slaDeadline
 *
 * Fallback ordering (when recommended technician rejected or dispatch failed):
 *   same-site first, then fewest active tickets, then code ascending.
 */
public class TechnicianSelectionPolicy {

    /**
     * Evaluates whether a dispatch-recommended technician passes all hard eligibility rules
     * AND the SLA timing rule.
     *
     * @param technician        candidate (with activeTicketCount already set)
     * @param category          ticket category
     * @param priority          ticket priority
     * @param ticketSite        site of the ticket
     * @param slaDeadline       the computed deadline
     * @param now               current time
     * @param estimatedHours    tiempoEstimadoHoras from dispatch
     * @return true if all rules pass
     */
    public boolean isRecommendedEligible(Technician technician, TicketCategory category,
                                         TicketPriority priority, String ticketSite,
                                         Instant slaDeadline, Instant now, int estimatedHours) {
        if (!passesHardRules(technician, category, priority, ticketSite)) return false;

        // SLA rule: estimated hours must fit within remaining deadline
        if (slaDeadline != null) {
            long remainingSeconds = slaDeadline.getEpochSecond() - now.getEpochSecond();
            long estimatedSeconds = (long) estimatedHours * 3600;
            if (estimatedSeconds > remainingSeconds) return false;
        }
        return true;
    }

    /**
     * Filters candidates by hard eligibility rules only (no SLA rule — per business-rules.md
     * the SLA rule is skipped for fallback candidates).
     */
    public List<Technician> getEligibleCandidates(List<Technician> candidates,
                                                   TicketCategory category,
                                                   TicketPriority priority,
                                                   String ticketSite) {
        return candidates.stream()
                .filter(t -> passesHardRules(t, category, priority, ticketSite))
                .toList();
    }

    /**
     * Selects the best fallback technician from already-eligible candidates.
     * Order: same site first, fewest active tickets, code ascending.
     */
    public Optional<Technician> selectFallback(List<Technician> eligible, String ticketSite) {
        return eligible.stream()
                .sorted(Comparator
                        .comparingInt((Technician t) -> t.getSite() != null
                                && t.getSite().equals(ticketSite) ? 0 : 1)
                        .thenComparingInt(Technician::getActiveTicketCount)
                        .thenComparing(Technician::getCode))
                .findFirst();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean passesHardRules(Technician t, TicketCategory category,
                                    TicketPriority priority, String ticketSite) {
        // Rule 1: must be active
        if (!t.isActive()) return false;

        // Rule 2: must be specialty-compatible (OTRO is wildcard)
        if (!t.isCompatibleWith(category)) return false;

        // Rule 3: must have capacity
        if (!t.hasCapacity()) return false;

        // Rule 4: site rule
        // ALTA/CRITICA: technician site must equal ticket site (hard)
        if ((priority == TicketPriority.ALTA || priority == TicketPriority.CRITICA)
                && !siteMatches(t.getSite(), ticketSite)) {
            return false;
        }
        // BAJA/MEDIA: same site preferred but not required (soft — handled by fallback ordering)

        return true;
    }

    private boolean siteMatches(String techSite, String ticketSite) {
        if (ticketSite == null || ticketSite.isBlank()) return true; // no site constraint
        return ticketSite.equalsIgnoreCase(techSite);
    }
}
