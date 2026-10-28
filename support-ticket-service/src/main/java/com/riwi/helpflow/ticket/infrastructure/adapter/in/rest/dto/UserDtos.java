package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto;

import com.riwi.helpflow.ticket.domain.model.Role;
import com.riwi.helpflow.ticket.domain.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class UserDtos {
    private UserDtos() {}

    public record UserResponse(
            UUID id,
            String document,
            String fullName,
            String email,
            String site,
            String department,
            Role role,
            UserStatus status
    ) {}

    public record UpdateUserRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String site,
            String department
    ) {}

    public record UpdateStatusRequest(
            UserStatus status
    ) {}
}
