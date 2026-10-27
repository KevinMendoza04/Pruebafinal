package com.riwi.helpflow.ticket.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Rich domain model for a system user.
 * No Spring / Jakarta / Jackson / Lombok imports allowed in domain.
 */
public class User {

    private final UUID id;
    private String document;
    private String fullName;
    private String email;
    private String site;
    private String department;
    private Role role;
    private UserStatus status;
    /** Hashed password — stored but never returned in responses. */
    private String passwordHash;

    private User(UUID id, String document, String fullName, String email,
                 String site, String department, Role role, UserStatus status,
                 String passwordHash) {
        this.id           = Objects.requireNonNull(id, "id required");
        this.document     = Objects.requireNonNull(document, "document required");
        this.fullName     = Objects.requireNonNull(fullName, "fullName required");
        this.email        = Objects.requireNonNull(email, "email required");
        this.site         = site;
        this.department   = department;
        this.role         = Objects.requireNonNull(role, "role required");
        this.status       = Objects.requireNonNull(status, "status required");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash required");
    }

    /** Factory for new registrations (always ROLE_USUARIO, always ACTIVO). */
    public static User register(UUID id, String document, String fullName, String email,
                                String site, String department, String passwordHash) {
        return new User(id, document, fullName, email, site, department,
                Role.ROLE_USUARIO, UserStatus.ACTIVO, passwordHash);
    }

    /** Factory used internally (admin bootstrap, technician creation). */
    public static User create(UUID id, String document, String fullName, String email,
                              String site, String department, Role role,
                              UserStatus status, String passwordHash) {
        return new User(id, document, fullName, email, site, department,
                role, status, passwordHash);
    }

    public void deactivate() { this.status = UserStatus.INACTIVO; }
    public void activate()   { this.status = UserStatus.ACTIVO; }

    public void updateProfile(String fullName, String email, String site, String department) {
        this.fullName   = Objects.requireNonNull(fullName);
        this.email      = Objects.requireNonNull(email);
        this.site       = site;
        this.department = department;
    }

    public boolean isActive() { return UserStatus.ACTIVO == this.status; }

    // ── Getters ──────────────────────────────────────────────────────────────
    public UUID       getId()           { return id; }
    public String     getDocument()     { return document; }
    public String     getFullName()     { return fullName; }
    public String     getEmail()        { return email; }
    public String     getSite()         { return site; }
    public String     getDepartment()   { return department; }
    public Role       getRole()         { return role; }
    public UserStatus getStatus()       { return status; }
    public String     getPasswordHash() { return passwordHash; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return id.equals(u.id);
    }
    @Override public int hashCode() { return id.hashCode(); }
}
