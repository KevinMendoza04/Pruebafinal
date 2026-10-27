package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates SLA deadlines and validates SLA policy cross-constraints.
 * Pure domain service — no Spring, no persistence.
 *
 * WHY cross-validation: business-rules.md requires CRITICA < ALTA < MEDIA < BAJA
 * (stricter priorities must have shorter windows).
 */
public class SlaCalculator {

    /**
     * Computes the SLA deadline given the ticket creation time and applicable policies.
     *
     * @param createdAt current time (from injected Clock)
     * @param priority  ticket priority
     * @param policies  all SLA policies (must contain an entry for the given priority)
     * @return the computed deadline
     */
    public Instant computeDeadline(Instant createdAt, TicketPriority priority,
                                   List<SlaPolicy> policies) {
        int hours = policies.stream()
                .filter(p -> p.getPriority() == priority)
                .mapToInt(SlaPolicy::getResolutionHours)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay política SLA para la prioridad: " + priority));
        return createdAt.plusSeconds((long) hours * 3600);
    }

    /**
     * Validates that the proposed SLA policies satisfy the ordering constraint:
     * CRITICA < ALTA < MEDIA < BAJA (hours strictly increasing).
     *
     * @param policies map from priority to proposed hours
     * @throws com.riwi.helpflow.ticket.domain.exception.InvalidSlaConfigException if invalid
     */
    public void validatePolicies(Map<TicketPriority, Integer> policies) {
        int critica = getOrDefault(policies, TicketPriority.CRITICA, Integer.MAX_VALUE);
        int alta    = getOrDefault(policies, TicketPriority.ALTA,    Integer.MAX_VALUE);
        int media   = getOrDefault(policies, TicketPriority.MEDIA,   Integer.MAX_VALUE);
        int baja    = getOrDefault(policies, TicketPriority.BAJA,    Integer.MAX_VALUE);

        if (!(critica < alta && alta < media && media < baja)) {
            throw new com.riwi.helpflow.ticket.domain.exception.InvalidSlaConfigException(
                    "Las horas SLA deben ser estrictamente crecientes: CRITICA < ALTA < MEDIA < BAJA");
        }
    }

    private int getOrDefault(Map<TicketPriority, Integer> map, TicketPriority key, int def) {
        return map.getOrDefault(key, def);
    }
}
