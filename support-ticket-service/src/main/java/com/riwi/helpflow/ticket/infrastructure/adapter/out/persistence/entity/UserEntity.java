package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import com.riwi.helpflow.ticket.domain.model.Role;
import com.riwi.helpflow.ticket.domain.model.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_users_document", columnNames = "document"),
           @UniqueConstraint(name = "uk_users_email",    columnNames = "email")
       })
@Getter @Setter
public class UserEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String document;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    private String site;
    private String department;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private UserStatus status;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
