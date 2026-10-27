package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Technician;
import com.riwi.helpflow.ticket.domain.model.TechnicianStatus;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ManageTechniciansUseCase {
    record CreateCommand(String document, String fullName, String email, String site,
                         String department, String code, Set<TicketCategory> specialties,
                         String supportProfile, int maxActiveTickets, String rawPassword) {}
    record UpdateCommand(String fullName, String email, String site, String department,
                         Set<TicketCategory> specialties, String supportProfile, int maxActiveTickets) {}

    Technician createTechnician(AuthenticatedActor actor, CreateCommand cmd);
    List<Technician> listTechnicians(AuthenticatedActor actor, int page, int size);
    Technician getTechnicianById(AuthenticatedActor actor, UUID id);
    Technician updateTechnician(AuthenticatedActor actor, UUID id, UpdateCommand cmd);
    Technician changeTechnicianStatus(AuthenticatedActor actor, UUID id, TechnicianStatus newStatus);
    Technician getMyProfile(AuthenticatedActor actor);
}
