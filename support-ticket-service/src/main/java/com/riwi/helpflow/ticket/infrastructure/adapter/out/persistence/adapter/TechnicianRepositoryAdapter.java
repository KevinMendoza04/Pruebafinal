package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.TechnicianRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.Technician;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TicketEntity;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.AssignmentJpaRepository;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.TechnicianJpaRepository;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TechnicianRepositoryAdapter implements TechnicianRepositoryPort {
    private final TechnicianJpaRepository repo;
    private final TicketJpaRepository ticketRepo;
    private final DomainEntityMapper mapper;

    public TechnicianRepositoryAdapter(TechnicianJpaRepository repo,
                                       TicketJpaRepository ticketRepo,
                                       DomainEntityMapper mapper) {
        this.repo = repo; this.ticketRepo = ticketRepo; this.mapper = mapper;
    }

    @Override public Technician save(Technician d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public Optional<Technician> findById(UUID id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<Technician> findByCode(String code) { return repo.findByCode(code).map(mapper::toDomain); }
    @Override public Optional<Technician> findByUserId(UUID userId) { return repo.findByUserId(userId).map(this::withActiveCount); }
    @Override public List<Technician> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size)).stream().map(this::withActiveCount).toList();
    }

    @Override
    public List<Technician> findActiveByCategory(TicketCategory category) {
        return repo.findAllActive().stream()
                .filter(t -> category == TicketCategory.OTRO || t.getSpecialties().contains(category))
                .map(this::withActiveCount)
                .toList();
    }

    @Override
    public Technician lockForUpdate(UUID id) {
        return repo.lockForUpdate(id).map(this::withActiveCount).orElse(null);
    }

    @Override public boolean existsByCode(String code) { return repo.existsByCode(code); }
    @Override public boolean existsByUserId(UUID userId) { return repo.existsByUserId(userId); }

    /** Populate active ticket count (ASIGNADO + EN_PROGRESO) for this technician. */
    private Technician withActiveCount(com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.TechnicianEntity e) {
        Technician t = mapper.toDomain(e);
        long count = ticketRepo.findByAssignedTechnicianId(e.getId(), PageRequest.of(0, 1000))
                .stream()
                .filter(te -> te.getStatus() == TicketStatus.ASIGNADO || te.getStatus() == TicketStatus.EN_PROGRESO)
                .count();
        t.setActiveTicketCount((int) count);
        return t;
    }
}
