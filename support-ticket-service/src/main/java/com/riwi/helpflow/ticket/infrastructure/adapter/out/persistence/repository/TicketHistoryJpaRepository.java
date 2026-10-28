package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketHistoryJpaRepository extends JpaRepository<TicketHistoryEntity, UUID> {
    List<TicketHistoryEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
