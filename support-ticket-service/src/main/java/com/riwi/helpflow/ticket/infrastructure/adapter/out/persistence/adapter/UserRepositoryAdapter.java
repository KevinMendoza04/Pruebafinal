package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.adapter;

import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.mapper.DomainEntityMapper;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository repo;
    private final DomainEntityMapper mapper;

    public UserRepositoryAdapter(UserJpaRepository repo, DomainEntityMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    @Override public User save(User d) { return mapper.toDomain(repo.save(mapper.toEntity(d))); }
    @Override public Optional<User> findById(UUID id) { return repo.findById(id).map(mapper::toDomain); }
    @Override public Optional<User> findByEmail(String email) { return repo.findByEmail(email).map(mapper::toDomain); }
    @Override public Optional<User> findByDocument(String doc) { return repo.findByDocument(doc).map(mapper::toDomain); }
    @Override public List<User> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }
    @Override public boolean existsByEmail(String email) { return repo.existsByEmail(email); }
    @Override public boolean existsByDocument(String doc) { return repo.existsByDocument(doc); }
}
