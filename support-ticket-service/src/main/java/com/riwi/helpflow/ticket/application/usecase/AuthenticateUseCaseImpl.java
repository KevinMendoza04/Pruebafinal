package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.AuthenticateUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.User;

public class AuthenticateUseCaseImpl implements AuthenticateUseCase {

    private final UserRepositoryPort userRepo;
    private final PasswordHasherPort hasher;
    private final TokenIssuerPort    tokenIssuer;
    private final TicketMetricsPort  metrics;
    private final long               expiresInSeconds;

    public AuthenticateUseCaseImpl(UserRepositoryPort userRepo, PasswordHasherPort hasher,
                                   TokenIssuerPort tokenIssuer, TicketMetricsPort metrics,
                                   long expiresInSeconds) {
        this.userRepo         = userRepo;
        this.hasher           = hasher;
        this.tokenIssuer      = tokenIssuer;
        this.metrics          = metrics;
        this.expiresInSeconds = expiresInSeconds;
    }

    @Override
    public Result authenticate(Command command) {
        User user = userRepo.findByEmail(command.email())
                .orElseThrow(() -> {
                    metrics.recordAuthFailure("user-not-found");
                    return new ResourceNotFoundException(
                        "Credenciales inválidas");
                });

        if (!hasher.matches(command.rawPassword(), user.getPasswordHash())) {
            metrics.recordAuthFailure("bad-password");
            throw new ForbiddenOperationException("Credenciales inválidas");
        }
        if (!user.isActive()) {
            metrics.recordAuthFailure("inactive");
            throw new ForbiddenOperationException(
                    "La cuenta está inactiva. Contacte al administrador.");
        }
        String token = tokenIssuer.issue(user);
        return new Result(token, "Bearer", expiresInSeconds);
    }
}
