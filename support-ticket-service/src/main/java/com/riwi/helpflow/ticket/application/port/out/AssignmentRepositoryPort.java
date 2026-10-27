package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.Assignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepositoryPort {
    Assignment save(Assignment assignment);
    Optional<Assignment> findActiveByTicketId(UUID ticketId);
    List<Assignment> findByTicketId(UUID ticketId);
}
