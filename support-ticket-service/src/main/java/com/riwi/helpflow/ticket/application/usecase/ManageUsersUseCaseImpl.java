package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.ManageUsersUseCase;
import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.domain.model.UserStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ManageUsersUseCaseImpl implements ManageUsersUseCase {

    private final UserRepositoryPort userRepo;

    public ManageUsersUseCaseImpl(UserRepositoryPort userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User getMe(AuthenticatedActor actor) {
        return userRepo.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado"));
    }

    @Override
    @Transactional
    public User updateMe(AuthenticatedActor actor, String fullName, String email,
                         String site, String department) {
        User user = userRepo.findById(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        user.updateProfile(fullName, email, site, department);
        return userRepo.save(user);
    }

    @Override
    public List<User> listUsers(AuthenticatedActor actor, int page, int size) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        return userRepo.findAll(page, size);
    }

    @Override
    public User getUserById(AuthenticatedActor actor, UUID id) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    @Override
    @Transactional
    public User updateUser(AuthenticatedActor actor, UUID id, String fullName, String email,
                           String site, String department) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        user.updateProfile(fullName, email, site, department);
        return userRepo.save(user);
    }

    @Override
    @Transactional
    public User changeUserStatus(AuthenticatedActor actor, UUID id, UserStatus newStatus) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        if (newStatus == UserStatus.ACTIVO)   user.activate();
        else                                   user.deactivate();
        return userRepo.save(user);
    }
}
