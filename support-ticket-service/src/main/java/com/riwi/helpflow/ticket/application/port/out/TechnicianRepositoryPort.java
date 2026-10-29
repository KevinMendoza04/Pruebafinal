package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.Technician;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicianRepositoryPort {
    Technician save(Technician technician);
    Optional<Technician> findById(UUID id);
    Optional<Technician> findByCode(String code);
    Optional<Technician> findByUserId(UUID userId);
    List<Technician> findAll(int page, int size);
    /** Returns all active technicians compatible with the given category, with activeTicketCount populated. */
    List<Technician> findActiveByCategory(TicketCategory category);
    /** Lock the technician row for update (pessimistic lock during assignment). */
    Technician lockForUpdate(UUID id);
    boolean existsByCode(String code);
    boolean existsByUserId(UUID userId);
    long count();
}
