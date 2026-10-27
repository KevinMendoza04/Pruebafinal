package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.time.Duration;

/**
 * Outbound port for reporting domain events as metrics.
 * Micrometer is NEVER imported in the domain or application layers.
 * The infrastructure adapter (MicrometerTicketMetricsAdapter) implements this.
 */
public interface TicketMetricsPort {
    void ticketCreated(TicketCategory category, TicketPriority priority);
    void recordFirstResponse(Duration elapsed);
    void recordResolution(Duration elapsed);
    void recordSlaBreached();
    void recordEscalated();
    void recordDispatchFailure();
    void recordAuthFailure(String reason);
    void recordAccessDenied();
}
