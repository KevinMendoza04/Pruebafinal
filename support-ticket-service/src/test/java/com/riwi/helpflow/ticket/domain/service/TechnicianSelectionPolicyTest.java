package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class TechnicianSelectionPolicyTest {

    private TechnicianSelectionPolicy policy;
    private Instant now;
    private Instant deadline;

    @BeforeEach void setUp() {
        policy   = new TechnicianSelectionPolicy();
        now      = Instant.parse("2025-01-01T10:00:00Z");
        deadline = now.plusSeconds(4 * 3600); // 4h SLA
    }

    private Technician techWith(String code, String site, Set<TicketCategory> specialties,
                                TechnicianStatus status, int activeCount) {
        Technician t = Technician.create(UUID.randomUUID(), UUID.randomUUID(), code,
                specialties, site, "profile", status, 5);
        t.setActiveTicketCount(activeCount);
        return t;
    }

    // ── isRecommendedEligible ─────────────────────────────────────────────────

    @Test @DisplayName("Recommended: active, compatible, capacity, same site, ETA fits → eligible")
    void recommended_allRulesPass() {
        Technician t = techWith("TEC-001", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 3)).isTrue(); // 3h < 4h remaining
    }

    @Test @DisplayName("Recommended: INACTIVO technician → not eligible")
    void recommended_inactiveTechnician() {
        Technician t = techWith("TEC-002", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.INACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 2)).isFalse();
    }

    @Test @DisplayName("Recommended: incompatible specialty → not eligible")
    void recommended_incompatibleSpecialty() {
        Technician t = techWith("TEC-003", "BOGOTA", Set.of(TicketCategory.HARDWARE),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 2)).isFalse();
    }

    @Test @DisplayName("Recommended: OTRO category is wildcard — any specialty qualifies")
    void recommended_otroIsWildcard() {
        Technician t = techWith("TEC-004", "BOGOTA", Set.of(TicketCategory.HARDWARE),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.OTRO, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 1)).isTrue();
    }

    @Test @DisplayName("Recommended: at capacity → not eligible")
    void recommended_atCapacity() {
        Technician t = techWith("TEC-005", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 5); // maxActive=5
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 2)).isFalse();
    }

    @Test @DisplayName("Recommended: ALTA priority — different site → not eligible (hard rule)")
    void recommended_altaPriorityDifferentSite() {
        Technician t = techWith("TEC-006", "MEDELLIN", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 2)).isFalse();
    }

    @Test @DisplayName("Recommended: MEDIA priority — different site still eligible (soft rule)")
    void recommended_mediaDifferentSiteAllowed() {
        Technician t = techWith("TEC-007", "MEDELLIN", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.MEDIA,
                "BOGOTA", deadline, now, 2)).isTrue();
    }

    @Test @DisplayName("Recommended: ETA exceeds remaining SLA → not eligible")
    void recommended_etaExceedsSla() {
        Technician t = techWith("TEC-008", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 0);
        assertThat(policy.isRecommendedEligible(t, TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", deadline, now, 5)).isFalse(); // 5h > 4h remaining
    }

    // ── Fallback ordering ────────────────────────────────────────────────────

    @Test @DisplayName("Fallback: same-site preferred over different-site")
    void fallback_sameSiteFirst() {
        Technician remote = techWith("TEC-010", "MEDELLIN", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 1);
        Technician local  = techWith("TEC-011", "BOGOTA",   Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 2); // more active tickets but same site
        Optional<Technician> selected = policy.selectFallback(List.of(remote, local), "BOGOTA");
        assertThat(selected).isPresent().contains(local);
    }

    @Test @DisplayName("Fallback: among same site, fewest active tickets wins")
    void fallback_fewestActiveTickets() {
        Technician busy  = techWith("TEC-012", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 3);
        Technician light = techWith("TEC-013", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 1);
        Optional<Technician> selected = policy.selectFallback(List.of(busy, light), "BOGOTA");
        assertThat(selected).isPresent().contains(light);
    }

    @Test @DisplayName("Fallback: tie on site and count → code ascending wins")
    void fallback_codeAscending() {
        Technician b = techWith("TEC-015", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 2);
        Technician a = techWith("TEC-014", "BOGOTA", Set.of(TicketCategory.RED),
                TechnicianStatus.ACTIVO, 2);
        Optional<Technician> selected = policy.selectFallback(List.of(b, a), "BOGOTA");
        assertThat(selected).isPresent();
        assertThat(selected.get().getCode()).isEqualTo("TEC-014");
    }

    @Test @DisplayName("Fallback: empty list returns empty")
    void fallback_emptyList() {
        assertThat(policy.selectFallback(List.of(), "BOGOTA")).isEmpty();
    }
}
