package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDocument(String document);
    List<User> findAll(int page, int size);
    boolean existsByEmail(String email);
    boolean existsByDocument(String document);
}
