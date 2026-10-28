package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.TicketHistoryRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.TicketHistoryEntry;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.TicketHistoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TicketHistoryRepositoryAdapter implements TicketHistoryRepositoryPort {
    private final TicketHistoryJpaRepository repo;
    private final DomainEntityMapper mapper;

    public TicketHistoryRepositoryAdapter(TicketHistoryJpaRepository repo, DomainEntityMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    @Override public TicketHistoryEntry save(TicketHistoryEntry d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public List<TicketHistoryEntry> findByTicketId(UUID ticketId) {
        return repo.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(mapper::toDomain).toList();
    }
}
