package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets",
       indexes = {
           @Index(name = "idx_tickets_status",      columnList = "status"),
           @Index(name = "idx_tickets_sla_deadline", columnList = "sla_deadline"),
           @Index(name = "idx_tickets_requester",    columnList = "requester_id"),
           @Index(name = "idx_tickets_technician",   columnList = "assigned_technician_id")
       })
@Getter @Setter
public class TicketEntity {
    @Id
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(nullable = false) private String title;
    @Column(nullable = false, length = 2000) private String description;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketCategory category;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketPriority priority;

    private String site;
    @Column(name = "affected_asset") private String affectedAsset;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "assigned_technician_id")
    private UUID assignedTechnicianId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Column(name = "sla_breached_at")
    private Instant slaBreachedAt;

    @Version
    private Long version;
}
