package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.ManageTechniciansUseCase;
import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.DuplicateResourceException;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.exception.ResourceNotFoundException;
import com.riwi.helpflow.ticket.domain.model.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public class ManageTechniciansUseCaseImpl implements ManageTechniciansUseCase {

    private final TechnicianRepositoryPort technicianRepo;
    private final UserRepositoryPort       userRepo;
    private final PasswordHasherPort       hasher;

    public ManageTechniciansUseCaseImpl(TechnicianRepositoryPort technicianRepo,
                                        UserRepositoryPort userRepo,
                                        PasswordHasherPort hasher) {
        this.technicianRepo = technicianRepo;
        this.userRepo       = userRepo;
        this.hasher         = hasher;
    }

    @Override
    @Transactional
    public Technician createTechnician(AuthenticatedActor actor, CreateCommand cmd) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        if (technicianRepo.existsByCode(cmd.code()))
            throw new DuplicateResourceException("Código de técnico ya existe: " + cmd.code());
        if (userRepo.existsByEmail(cmd.email()))
            throw new DuplicateResourceException("Email ya registrado: " + cmd.email());
        if (userRepo.existsByDocument(cmd.document()))
            throw new DuplicateResourceException("Documento ya registrado: " + cmd.document());

        // Create the associated user atomically
        User user = User.create(UUID.randomUUID(), cmd.document(), cmd.fullName(),
                cmd.email(), cmd.site(), cmd.department(),
                Role.ROLE_TECNICO, UserStatus.ACTIVO,
                hasher.hash(cmd.rawPassword()));
        userRepo.save(user);

        Technician tech = Technician.create(UUID.randomUUID(), user.getId(), cmd.code(),
                cmd.specialties(), cmd.site(), cmd.supportProfile(),
                TechnicianStatus.ACTIVO, cmd.maxActiveTickets());
        return technicianRepo.save(tech);
    }

    @Override
    public List<Technician> listTechnicians(AuthenticatedActor actor, int page, int size) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        return technicianRepo.findAll(page, size);
    }

    @Override
    public Technician getTechnicianById(AuthenticatedActor actor, UUID id) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        return technicianRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico no encontrado: " + id));
    }

    @Override
    @Transactional
    public Technician updateTechnician(AuthenticatedActor actor, UUID id, UpdateCommand cmd) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        Technician tech = technicianRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico no encontrado: " + id));
        // Update user profile as well
        userRepo.findById(tech.getUserId()).ifPresent(u -> {
            u.updateProfile(cmd.fullName(), cmd.email(), cmd.site(), cmd.department());
            userRepo.save(u);
        });
        // Rebuild technician with new values (via a new create — immutable fields stay)
        Technician updated = Technician.create(tech.getId(), tech.getUserId(), tech.getCode(),
                cmd.specialties(), cmd.site(), cmd.supportProfile(),
                tech.getStatus(), cmd.maxActiveTickets());
        return technicianRepo.save(updated);
    }

    @Override
    @Transactional
    public Technician changeTechnicianStatus(AuthenticatedActor actor, UUID id,
                                             TechnicianStatus newStatus) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        Technician tech = technicianRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Técnico no encontrado: " + id));
        if (newStatus == TechnicianStatus.ACTIVO)   tech.activate();
        else                                         tech.deactivate();
        return technicianRepo.save(tech);
    }

    @Override
    public Technician getMyProfile(AuthenticatedActor actor) {
        if (!actor.isTechnician()) throw new ForbiddenOperationException("Solo técnicos");
        return technicianRepo.findByUserId(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de técnico no encontrado para el usuario: " + actor.userId()));
    }
}
