package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.AssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity, UUID> {
    Optional<AssignmentEntity> findByTicketIdAndUnassignedAtIsNull(UUID ticketId);
    List<AssignmentEntity> findByTicketId(UUID ticketId);
}
