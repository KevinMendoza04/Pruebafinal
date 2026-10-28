package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper;

import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Manual entity<->domain mapper.
 * WHY not MapStruct here: the domain uses rich factory methods (User.register, Ticket.create,
 * Ticket.reconstitute, Technician.create) and immutable-ish semantics that MapStruct cannot
 * target cleanly. The steering explicitly allows a small explicit mapper with justification.
 */
@Component
public class DomainEntityMapper {

    // ── User ───────────────────────────────────────────────────────────────
    public User toDomain(UserEntity e) {
        if (e == null) return null;
        return User.create(e.getId(), e.getDocument(), e.getFullName(), e.getEmail(),
                e.getSite(), e.getDepartment(), e.getRole(), e.getStatus(),
                e.getPasswordHash());
    }
    public UserEntity toEntity(User d) {
        if (d == null) return null;
        UserEntity e = new UserEntity();
        e.setId(d.getId());
        e.setDocument(d.getDocument());
        e.setFullName(d.getFullName());
        e.setEmail(d.getEmail());
        e.setSite(d.getSite());
        e.setDepartment(d.getDepartment());
        e.setRole(d.getRole());
        e.setStatus(d.getStatus());
        e.setPasswordHash(d.getPasswordHash());
        return e;
    }

    // ── Technician ─────────────────────────────────────────────────────────
    public Technician toDomain(TechnicianEntity e) {
        if (e == null) return null;
        Set<TicketCategory> specs = e.getSpecialties() == null
                ? EnumSet.noneOf(TicketCategory.class)
                : EnumSet.copyOf(e.getSpecialties());
        return Technician.create(e.getId(), e.getUserId(), e.getCode(),
                specs, e.getSite(), e.getSupportProfile(), e.getStatus(),
                e.getMaxActiveTickets());
    }
    public TechnicianEntity toEntity(Technician d) {
        if (d == null) return null;
        TechnicianEntity e = new TechnicianEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setCode(d.getCode());
        e.setSpecialties(d.getSpecialties());
        e.setSite(d.getSite());
        e.setSupportProfile(d.getSupportProfile());
        e.setStatus(d.getStatus());
        e.setMaxActiveTickets(d.getMaxActiveTickets());
        return e;
    }

    // ── Ticket ────────────────────────────────────────────────────────────
    public Ticket toDomain(TicketEntity e) {
        if (e == null) return null;
        return Ticket.reconstitute(e.getId(), e.getRequesterId(), e.getTitle(),
                e.getDescription(), e.getCategory(), e.getPriority(), e.getSite(),
                e.getAffectedAsset(), e.getStatus(), e.getAssignedTechnicianId(),
                e.getCreatedAt(), e.getUpdatedAt(), e.getSlaDeadline(),
                e.getFirstRespondedAt(), e.getResolvedAt(), e.getEscalatedAt(),
                e.getSlaBreachedAt(), e.getVersion());
    }
    public TicketEntity toEntity(Ticket d) {
        if (d == null) return null;
        TicketEntity e = new TicketEntity();
        e.setId(d.getId());
        e.setRequesterId(d.getRequesterId());
        e.setTitle(d.getTitle());
        e.setDescription(d.getDescription());
        e.setCategory(d.getCategory());
        e.setPriority(d.getPriority());
        e.setSite(d.getSite());
        e.setAffectedAsset(d.getAffectedAsset());
        e.setStatus(d.getStatus());
        e.setAssignedTechnicianId(d.getAssignedTechnicianId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        e.setSlaDeadline(d.getSlaDeadline());
        e.setFirstRespondedAt(d.getFirstRespondedAt());
        e.setResolvedAt(d.getResolvedAt());
        e.setEscalatedAt(d.getEscalatedAt());
        e.setSlaBreachedAt(d.getSlaBreachedAt());
        e.setVersion(d.getVersion());
        return e;
    }

    // ── TicketHistoryEntry ───────────────────────────────────────────────
    public TicketHistoryEntry toDomain(TicketHistoryEntity e) {
        if (e == null) return null;
        return TicketHistoryEntry.of(e.getId(), e.getTicketId(), e.getEventType(),
                e.getFromStatus(), e.getToStatus(), e.getActorId(),
                e.getObservation(), e.getCreatedAt());
    }
    public TicketHistoryEntity toEntity(TicketHistoryEntry d) {
        if (d == null) return null;
        TicketHistoryEntity e = new TicketHistoryEntity();
        e.setId(d.getId());
        e.setTicketId(d.getTicketId());
        e.setEventType(d.getEventType());
        e.setFromStatus(d.getFromStatus());
        e.setToStatus(d.getToStatus());
        e.setActorId(d.getActorId());
        e.setObservation(d.getObservation());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    // ── Assignment ────────────────────────────────────────────────────────
    public Assignment toDomain(AssignmentEntity e) {
        if (e == null) return null;
        Assignment a = Assignment.create(e.getId(), e.getTicketId(),
                e.getTechnicianId(), e.getAssignedAt());
        if (e.getUnassignedAt() != null) a.close(e.getUnassignedAt());
        return a;
    }
    public AssignmentEntity toEntity(Assignment d) {
        if (d == null) return null;
        AssignmentEntity e = new AssignmentEntity();
        e.setId(d.getId());
        e.setTicketId(d.getTicketId());
        e.setTechnicianId(d.getTechnicianId());
        e.setAssignedAt(d.getAssignedAt());
        e.setUnassignedAt(d.getUnassignedAt());
        return e;
    }

    // ── SlaPolicy ─────────────────────────────────────────────────────────
    public SlaPolicy toDomain(SlaPolicyEntity e) {
        if (e == null) return null;
        return SlaPolicy.of(e.getId(), e.getPriority(), e.getResolutionHours());
    }
    public SlaPolicyEntity toEntity(SlaPolicy d) {
        if (d == null) return null;
        SlaPolicyEntity e = new SlaPolicyEntity();
        e.setId(d.getId());
        e.setPriority(d.getPriority());
        e.setResolutionHours(d.getResolutionHours());
        return e;
    }
}
