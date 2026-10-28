package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import com.riwi.helpflow.ticket.domain.model.TechnicianStatus;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "technicians",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_technicians_code",    columnNames = "code"),
           @UniqueConstraint(name = "uk_technicians_user_id", columnNames = "user_id")
       })
@Getter @Setter
public class TechnicianEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String code;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = TicketCategory.class)
    @CollectionTable(name = "technician_specialties",
                     joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "specialty")
    @Enumerated(EnumType.STRING)
    private Set<TicketCategory> specialties = EnumSet.noneOf(TicketCategory.class);

    private String site;
    @Column(name = "support_profile", length = 1000)
    private String supportProfile;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TechnicianStatus status;

    @Column(name = "max_active_tickets", nullable = false)
    private int maxActiveTickets;
}
