package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepositoryPort {
    SlaPolicy save(SlaPolicy policy);
    Optional<SlaPolicy> findByPriority(TicketPriority priority);
    List<SlaPolicy> findAll();
}
