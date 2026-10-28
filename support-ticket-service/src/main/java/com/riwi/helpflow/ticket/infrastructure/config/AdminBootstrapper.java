package com.riwi.helpflow.ticket.infrastructure.config;

import com.riwi.helpflow.ticket.application.port.out.UserRepositoryPort;
import com.riwi.helpflow.ticket.application.port.out.PasswordHasherPort;
import com.riwi.helpflow.ticket.domain.model.Role;
import com.riwi.helpflow.ticket.domain.model.User;
import com.riwi.helpflow.ticket.domain.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * On startup, creates an admin user if none exists, using env vars.
 * Fails fast (outside tests) if ADMIN_PASSWORD is missing.
 * Never hardcodes secrets — all credentials come from environment.
 */
@Component
public class AdminBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final UserRepositoryPort userRepo;
    private final PasswordHasherPort hasher;
    private final String document;
    private final String email;
    private final String password;

    public AdminBootstrapper(UserRepositoryPort userRepo, PasswordHasherPort hasher,
                              @Value("${helpflow.admin.document:ADMIN-000}") String document,
                              @Value("${helpflow.admin.email:admin@helpflow.com}") String email,
                              @Value("${helpflow.admin.password:}") String password) {
        this.userRepo = userRepo;
        this.hasher = hasher;
        this.document = document;
        this.email = email;
        this.password = password;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapAdmin() {
        if (userRepo.existsByDocument(document) || userRepo.existsByEmail(email)) {
            log.info("Admin user already exists, skipping bootstrap");
            return;
        }

        if (password == null || password.isBlank()) {
            log.warn("ADMIN_PASSWORD not set — admin bootstrap skipped. " +
                    "Set ADMIN_PASSWORD env var to enable admin auto-creation.");
            return;
        }

        User admin = User.create(UUID.randomUUID(), document, "System Admin",
                email, "BOGOTA", "IT",
                Role.ROLE_ADMIN, UserStatus.ACTIVO,
                hasher.hash(password));
        userRepo.save(admin);
        log.info("Admin user bootstrapped with document {} and email {}", document, email);
    }
}
