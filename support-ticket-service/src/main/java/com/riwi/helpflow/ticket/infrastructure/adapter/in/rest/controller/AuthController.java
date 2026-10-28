package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.controller;

import com.riwi.helpflow.ticket.application.port.in.AuthenticateUseCase;
import com.riwi.helpflow.ticket.application.port.in.RegisterUserUseCase;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.AuthDtos;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.UserDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "User registration and authentication")
public class AuthController {

    private final RegisterUserUseCase registerUseCase;
    private final AuthenticateUseCase  loginUseCase;

    public AuthController(RegisterUserUseCase registerUseCase, AuthenticateUseCase loginUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase    = loginUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDtos.UserResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        User user = registerUseCase.register(new RegisterUserUseCase.Command(
                req.document(), req.fullName(), req.email(),
                req.site(), req.department(), req.password()));
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        AuthenticateUseCase.Result result = loginUseCase.authenticate(
                new AuthenticateUseCase.Command(req.email(), req.password()));
        return ResponseEntity.ok(new AuthDtos.AuthResponse(
                result.accessToken(), result.tokenType(), result.expiresIn()));
    }

    private UserDtos.UserResponse toResponse(User u) {
        return new UserDtos.UserResponse(u.getId(), u.getDocument(), u.getFullName(),
                u.getEmail(), u.getSite(), u.getDepartment(), u.getRole(), u.getStatus());
    }
}
