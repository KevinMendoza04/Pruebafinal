package com.riwi.helpflow.ticket.infrastructure.adapter.out.persistence.entity;

import com.riwi.helpflow.ticket.domain.model.HistoryEventType;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_history",
       indexes = @Index(name = "idx_ticket_history_ticket", columnList = "ticket_id"))
@Getter @Setter
public class TicketHistoryEntity {
    @Id
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private HistoryEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private TicketStatus toStatus;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(length = 1000)
    private String observation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
