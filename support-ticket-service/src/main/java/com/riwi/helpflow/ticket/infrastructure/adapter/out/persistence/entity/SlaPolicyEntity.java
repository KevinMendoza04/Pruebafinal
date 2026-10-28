package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sla_policies")
@Getter @Setter
public class SlaPolicyEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true)
    private TicketPriority priority;

    @Column(name = "resolution_hours", nullable = false)
    private int resolutionHours;
}
