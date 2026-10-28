package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.TicketJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TicketRepositoryAdapter implements TicketRepositoryPort {
    private final TicketJpaRepository repo;
    private final DomainEntityMapper mapper;

    public TicketRepositoryAdapter(TicketJpaRepository repo, DomainEntityMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    @Override public Ticket save(Ticket d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public Optional<Ticket> findById(UUID id) { return repo.findById(id).map(mapper::toDomain); }

    @Override public List<Ticket> findByRequesterId(UUID requesterId, int page, int size) {
        return repo.findByRequesterId(requesterId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override public List<Ticket> findByTechnicianId(UUID technicianId, int page, int size) {
        return repo.findByAssignedTechnicianId(technicianId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findAll(int page, int size, TicketStatus status, TicketPriority priority, TicketCategory category) {
        return repo.findAll(PageRequest.of(page, size))
                .stream().map(mapper::toDomain)
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> category == null || t.getCategory() == category)
                .toList();
    }

    @Override public List<Ticket> findOpenTickets() {
        return repo.findOpenTickets().stream().map(mapper::toDomain).toList();
    }

    @Override public long countOverdue(Instant now) { return repo.countOverdue(now); }
}
