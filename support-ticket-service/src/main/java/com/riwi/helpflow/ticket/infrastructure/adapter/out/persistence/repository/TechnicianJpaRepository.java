package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianJpaRepository extends JpaRepository<TechnicianEntity, UUID> {
    Optional<TechnicianEntity> findByCode(String code);
    Optional<TechnicianEntity> findByUserId(UUID userId);
    boolean existsByCode(String code);
    boolean existsByUserId(UUID userId);
    Page<TechnicianEntity> findAll(Pageable pageable);

    /** Active technicians with matching specialty (or OTRO wildcard handled in adapter). */
    @Query("SELECT t FROM TechnicianEntity t WHERE t.status = com.riwi.helpflow.ticket.domain.model.TechnicianStatus.ACTIVO")
    List<TechnicianEntity> findAllActive();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TechnicianEntity t WHERE t.id = :id")
    Optional<TechnicianEntity> lockForUpdate(UUID id);
}
