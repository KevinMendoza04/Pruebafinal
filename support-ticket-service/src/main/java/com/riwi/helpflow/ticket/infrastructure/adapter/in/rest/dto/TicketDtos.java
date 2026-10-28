package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto;

import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class TicketDtos {
    private TicketDtos() {}

    public record CreateTicketRequest(
            @NotBlank @Size(min = 5, max = 120, message = "El título debe tener entre 5 y 120 caracteres")
            String title,
            @NotBlank @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
            String description,
            @NotNull(message = "La categoría es obligatoria")
            TicketCategory category,
            @NotNull(message = "La prioridad es obligatoria")
            TicketPriority priority,
            String site,
            @NotBlank @Size(min = 2, max = 100, message = "El activo afectado debe tener entre 2 y 100 caracteres")
            String affectedAsset
    ) {}

    public record UpdateTicketRequest(
            @NotBlank @Size(min = 5, max = 120) String title,
            @NotBlank @Size(min = 10, max = 2000) String description,
            @Size(max = 100) String affectedAsset
    ) {}

    public record TransitionRequest(
            @NotNull TicketStatus targetStatus,
            String observation
    ) {}

    public record TicketResponse(
            UUID id,
            UUID requesterId,
            String title,
            String description,
            TicketCategory category,
            TicketPriority priority,
            String site,
            String affectedAsset,
            TicketStatus status,
            UUID assignedTechnicianId,
            Instant createdAt,
            Instant slaDeadline,
            Instant firstRespondedAt,
            Instant resolvedAt,
            Instant escalatedAt
    ) {}

    public record TicketHistoryResponse(
            UUID id,
            String eventType,
            String fromStatus,
            String toStatus,
            UUID actorId,
            String observation,
            Instant createdAt
    ) {}
}
