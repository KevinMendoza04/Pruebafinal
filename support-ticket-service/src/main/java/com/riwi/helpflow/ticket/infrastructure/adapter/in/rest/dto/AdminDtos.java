package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto;

import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.util.Map;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {}

    public record ReassignRequest(UUID newTechnicianId) {}

    public record SlaPoliciesRequest(Map<TicketPriority, Integer> policies) {}

    public record SlaPolicyResponse(
            UUID id,
            TicketPriority priority,
            int resolutionHours
    ) {}
}
