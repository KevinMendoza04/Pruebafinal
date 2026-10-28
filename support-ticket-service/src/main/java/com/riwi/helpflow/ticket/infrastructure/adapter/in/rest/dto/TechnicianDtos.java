package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto;

import com.riwi.helpflow.ticket.domain.model.TechnicianStatus;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public final class TechnicianDtos {
    private TechnicianDtos() {}

    public record CreateTechnicianRequest(
            @NotBlank String document,
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String site,
            String department,
            @NotBlank String code,
            @NotNull Set<TicketCategory> specialties,
            String supportProfile,
            Integer maxActiveTickets,
            @NotBlank String password
    ) {}

    public record UpdateTechnicianRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String site,
            String department,
            Set<TicketCategory> specialties,
            String supportProfile,
            Integer maxActiveTickets
    ) {}

    public record TechnicianResponse(
            UUID id,
            String code,
            Set<TicketCategory> specialties,
            String site,
            String supportProfile,
            TechnicianStatus status,
            int maxActiveTickets,
            UUID userId
    ) {}
}
