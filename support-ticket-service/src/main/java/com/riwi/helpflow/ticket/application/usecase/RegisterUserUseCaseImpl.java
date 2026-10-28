package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.RegisterUserUseCase;
import com.riwi.helpflow.ticket.application.port.out.PasswordHasherPort;
import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.DuplicateResourceException;
import com.riwi.helpflow.ticket.domain.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepositoryPort  userRepo;
    private final PasswordHasherPort  hasher;
    private final String             corporateEmailDomain;

    public RegisterUserUseCaseImpl(UserRepositoryPort userRepo, PasswordHasherPort hasher,
                                   String corporateEmailDomain) {
        this.userRepo = userRepo;
        this.hasher   = hasher;
        this.corporateEmailDomain = corporateEmailDomain;
    }

    @Override
    @Transactional
    public User register(Command command) {
        // Corporate email domain validation
        if (corporateEmailDomain != null && !corporateEmailDomain.isBlank()) {
            String domain = command.email().substring(command.email().indexOf('@') + 1);
            if (!domain.equalsIgnoreCase(corporateEmailDomain)) {
                throw new com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException(
                        "El correo debe pertenecer al dominio corporativo: " + corporateEmailDomain);
            }
        }

        if (userRepo.existsByEmail(command.email())) {
            throw new DuplicateResourceException(
                    "El correo ya está registrado: " + command.email());
        }
        if (userRepo.existsByDocument(command.document())) {
            throw new DuplicateResourceException(
                    "El documento ya está registrado: " + command.document());
        }
        String hash = hasher.hash(command.rawPassword());
        User user = User.register(UUID.randomUUID(), command.document(), command.fullName(),
                command.email(), command.site(), command.department(), hash);
        return userRepo.save(user);
    }
}
