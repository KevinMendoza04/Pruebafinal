package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.repository;

import com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByDocument(String document);
    boolean existsByEmail(String email);
    boolean existsByDocument(String document);
    Page<UserEntity> findAll(Pageable pageable);
}
