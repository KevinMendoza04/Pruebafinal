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

    public RegisterUserUseCaseImpl(UserRepositoryPort userRepo, PasswordHasherPort hasher) {
        this.userRepo = userRepo;
        this.hasher   = hasher;
    }

    @Override
    @Transactional
    public User register(Command command) {
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
