package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.SlaPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyJpaRepository extends JpaRepository<SlaPolicyEntity, UUID> {
    Optional<SlaPolicyEntity> findByPriority(
            com.riwi.helpflow.ticket.domain.model.TicketPriority priority);
}
