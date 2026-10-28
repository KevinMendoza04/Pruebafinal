package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.AssignmentRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.Assignment;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.AssignmentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AssignmentRepositoryAdapter implements AssignmentRepositoryPort {
    private final AssignmentJpaRepository repo;
    private final DomainEntityMapper mapper;

    public AssignmentRepositoryAdapter(AssignmentJpaRepository repo, DomainEntityMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    @Override public Assignment save(Assignment d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public Optional<Assignment> findActiveByTicketId(UUID ticketId) {
        return repo.findByTicketIdAndUnassignedAtIsNull(ticketId).map(mapper::toDomain);
    }
    @Override public List<Assignment> findByTicketId(UUID ticketId) {
        return repo.findByTicketId(ticketId).stream().map(mapper::toDomain).toList();
    }
}
