package com.riwi.helpflow.ticket.domain.service;

import com.riwi.helpflow.ticket.domain.exception.InvalidSlaConfigException;
import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SlaCalculatorTest {

    private SlaCalculator sla;
    private List<SlaPolicy> defaultPolicies;

    @BeforeEach void setUp() {
        sla = new SlaCalculator();
        defaultPolicies = List.of(
                SlaPolicy.of(UUID.randomUUID(), TicketPriority.CRITICA, 2),
                SlaPolicy.of(UUID.randomUUID(), TicketPriority.ALTA,    4),
                SlaPolicy.of(UUID.randomUUID(), TicketPriority.MEDIA,   8),
                SlaPolicy.of(UUID.randomUUID(), TicketPriority.BAJA,    24)
        );
    }

    @ParameterizedTest(name = "priority={0} -> +{1}h")
    @CsvSource({"CRITICA,2", "ALTA,4", "MEDIA,8", "BAJA,24"})
    @DisplayName("SLA deadline = createdAt + correct hours per priority")
    void deadline_correctHoursPerPriority(TicketPriority priority, int expectedHours) {
        Instant now = Instant.parse("2025-01-01T10:00:00Z");
        Instant deadline = sla.computeDeadline(now, priority, defaultPolicies);
        assertThat(deadline).isEqualTo(now.plusSeconds((long) expectedHours * 3600));
    }

    @Test @DisplayName("Missing policy throws IllegalArgumentException")
    void missingPolicy_throws() {
        assertThatThrownBy(() ->
                sla.computeDeadline(Instant.now(), TicketPriority.CRITICA, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("Valid SLA policy order passes validation")
    void validatePolicies_valid() {
        assertThatNoException().isThrownBy(() ->
                sla.validatePolicies(Map.of(
                        TicketPriority.CRITICA, 2,
                        TicketPriority.ALTA,    4,
                        TicketPriority.MEDIA,   8,
                        TicketPriority.BAJA,    24)));
    }

    @Test @DisplayName("ALTA >= CRITICA fails cross-validation")
    void validatePolicies_altaNotGreaterThanCritica() {
        assertThatThrownBy(() ->
                sla.validatePolicies(Map.of(
                        TicketPriority.CRITICA, 4,
                        TicketPriority.ALTA,    4,
                        TicketPriority.MEDIA,   8,
                        TicketPriority.BAJA,    24)))
                .isInstanceOf(InvalidSlaConfigException.class);
    }

    @Test @DisplayName("BAJA <= MEDIA fails cross-validation")
    void validatePolicies_bajaNotGreaterThanMedia() {
        assertThatThrownBy(() ->
                sla.validatePolicies(Map.of(
                        TicketPriority.CRITICA, 2,
                        TicketPriority.ALTA,    4,
                        TicketPriority.MEDIA,   8,
                        TicketPriority.BAJA,    8)))
                .isInstanceOf(InvalidSlaConfigException.class);
    }
}
