package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.controller;

import com.riwi.helpflow.ticket.application.port.in.ManageUsersUseCase;
import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.ActorResolver;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.UserDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final ManageUsersUseCase useCase;
    private final ActorResolver     actorResolver;

    public UserController(ManageUsersUseCase useCase, ActorResolver actorResolver) {
        this.useCase = useCase;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDtos.UserResponse> me(HttpServletRequest req) {
        return ResponseEntity.ok(toResponse(useCase.getMe(actorResolver.resolve(req))));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDtos.UserResponse> updateMe(
            @Valid @RequestBody UserDtos.UpdateUserRequest body, HttpServletRequest req) {
        AuthenticatedActor actor = actorResolver.resolve(req);
        User user = useCase.updateMe(actor, body.fullName(), body.email(), body.site(), body.department());
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserDtos.UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        return ResponseEntity.ok(
                useCase.listUsers(actorResolver.resolve(req), page, size)
                        .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> getById(@PathVariable UUID id, HttpServletRequest req) {
        return ResponseEntity.ok(toResponse(useCase.getUserById(actorResolver.resolve(req), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDtos.UserResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UserDtos.UpdateUserRequest body,
            HttpServletRequest req) {
        AuthenticatedActor actor = actorResolver.resolve(req);
        User user = useCase.updateUser(actor, id, body.fullName(), body.email(), body.site(), body.department());
        return ResponseEntity.ok(toResponse(user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDtos.UserResponse> changeStatus(
            @PathVariable UUID id, @RequestBody UserDtos.UpdateStatusRequest body,
            HttpServletRequest req) {
        User user = useCase.changeUserStatus(actorResolver.resolve(req), id, body.status());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserDtos.UserResponse toResponse(User u) {
        return new UserDtos.UserResponse(u.getId(), u.getDocument(), u.getFullName(),
                u.getEmail(), u.getSite(), u.getDepartment(), u.getRole(), u.getStatus());
    }
}
