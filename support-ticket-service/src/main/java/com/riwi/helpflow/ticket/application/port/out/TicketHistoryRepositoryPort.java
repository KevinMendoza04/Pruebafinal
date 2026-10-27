package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.TicketHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface TicketHistoryRepositoryPort {
    TicketHistoryEntry save(TicketHistoryEntry entry);
    List<TicketHistoryEntry> findByTicketId(UUID ticketId);
}
