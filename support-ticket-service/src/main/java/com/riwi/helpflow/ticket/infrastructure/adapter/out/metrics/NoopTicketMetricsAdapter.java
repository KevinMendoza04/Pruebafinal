package com.riwi.helpflow.ticket.infrastructure.adapter.out.metrics;

import com.riwi.helpflow.ticket.application.port.out.TicketMetricsPort;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Stub metrics adapter. Replaced by Micrometer-backed adapter in PROMPT 4.
 * WHY: keeps the application layer testable now without pulling Micrometer into tests.
 */
@Component
public class NoopTicketMetricsAdapter implements TicketMetricsPort {
    @Override public void ticketCreated(TicketCategory c, TicketPriority p) {}
    @Override public void recordFirstResponse(Duration d) {}
    @Override public void recordResolution(Duration d) {}
    @Override public void recordSlaBreached() {}
    @Override public void recordEscalated() {}
    @Override public void recordDispatchFailure() {}
    @Override public void recordAuthFailure(String reason) {}
    @Override public void recordAccessDenied() {}
}
