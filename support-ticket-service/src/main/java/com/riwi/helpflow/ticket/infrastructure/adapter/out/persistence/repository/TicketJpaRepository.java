package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID> {

    // Use EntityGraph to avoid N+1 when loading ticket details with relations
    @EntityGraph(attributePaths = {"requesterId", "assignedTechnicianId"})
    Optional<TicketEntity> findWithDetailsById(UUID id);

    Page<TicketEntity> findByRequesterId(UUID requesterId, Pageable pageable);
    Page<TicketEntity> findByAssignedTechnicianId(UUID technicianId, Pageable pageable);

    @Query("SELECT t FROM TicketEntity t WHERE t.status IN " +
           "(com.riwi.helpflow.ticket.domain.model.TicketStatus.ABIERTO, " +
           " com.riwi.helpflow.ticket.domain.model.TicketStatus.ASIGNADO, " +
           " com.riwi.helpflow.ticket.domain.model.TicketStatus.EN_PROGRESO)")
    List<TicketEntity> findOpenTickets();

    @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.slaDeadline < :now " +
           "AND t.status IN (com.riwi.helpflow.ticket.domain.model.TicketStatus.ABIERTO, " +
           " com.riwi.helpflow.ticket.domain.model.TicketStatus.ASIGNADO, " +
           " com.riwi.helpflow.ticket.domain.model.TicketStatus.EN_PROGRESO)")
    long countOverdue(@Param("now") Instant now);
}
