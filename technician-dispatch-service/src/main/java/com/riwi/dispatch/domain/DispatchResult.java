package com.riwi.dispatch.domain;

/**
 * Value object returned by the domain dispatcher.
 * Pure Java record — no Spring, no Jackson.
 *
 * @param technicianCode      e.g. "TEC-014"
 * @param estimatedHours      0..12
 * @param availability        derived from estimatedHours
 */
public record DispatchResult(
        String technicianCode,
        int estimatedHours,
        Availability availability
) {}
