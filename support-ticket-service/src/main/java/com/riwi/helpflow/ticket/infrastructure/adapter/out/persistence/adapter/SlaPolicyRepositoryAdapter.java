package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.SlaPolicyRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.SlaPolicyJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SlaPolicyRepositoryAdapter implements SlaPolicyRepositoryPort {
    private final SlaPolicyJpaRepository repo;
    private final DomainEntityMapper mapper;

    public SlaPolicyRepositoryAdapter(SlaPolicyJpaRepository repo, DomainEntityMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    @Override public SlaPolicy save(SlaPolicy d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public Optional<SlaPolicy> findByPriority(TicketPriority p) { return repo.findByPriority(p).map(mapper::toDomain); }
    @Override public List<SlaPolicy> findAll() { return repo.findAll().stream().map(mapper::toDomain).toList(); }
}
