package com.riwi.helpflow.ticket.infrastructure.adapter.out.metrics;

import com.riwi.helpflow.ticket.application.port.out.TicketMetricsPort;
import com.riwi.helpflow.ticket.application.port.out.TicketRepositoryPort;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer-backed metrics adapter implementing TicketMetricsPort.
 * Micrometer never leaks into the domain/application layers — only this adapter knows about it.
 *
 * Metrics:
 *   helpflow.tickets.created{category,priority}     — counter
 *   helpflow.tickets.first_response                  — timer
 *   helpflow.tickets.resolution                      — timer
 *   helpflow.tickets.overdue                         — gauge (current count)
 *   helpflow.sla.breaches                            — counter
 *   helpflow.tickets.escalated                       — counter
 *   helpflow.auth.failures{reason}                   — counter
 *   helpflow.access.denied                           — counter
 *   helpflow.dispatch.failures                       — counter
 */
@Component
public class MicrometerTicketMetricsAdapter implements TicketMetricsPort {

    private final MeterRegistry registry;
    private final Clock clock;
    private final TicketRepositoryPort ticketRepo;
    private final AtomicLong overdueGauge = new AtomicLong(0);

    public MicrometerTicketMetricsAdapter(MeterRegistry registry, Clock clock,
                                           TicketRepositoryPort ticketRepo) {
        this.registry = registry;
        this.clock = clock;
        this.ticketRepo = ticketRepo;

        // Register the overdue gauge (queries current count on demand)
        registry.gauge("helpflow.tickets.overdue", overdueGauge);
    }

    /** Refreshes the overdue gauge — called by the escalation scheduler. */
    public void refreshOverdueGauge() {
        overdueGauge.set(ticketRepo.countOverdue(clock.instant()));
    }

    @Override
    public void ticketCreated(TicketCategory category, TicketPriority priority) {
        Counter.builder("helpflow.tickets.created")
                .tags(Tags.of("category", category.name(), "priority", priority.name()))
                .register(registry).increment();
    }

    @Override
    public void recordFirstResponse(Duration elapsed) {
        Timer.builder("helpflow.tickets.first_response")
                .register(registry).record(elapsed);
    }

    @Override
    public void recordResolution(Duration elapsed) {
        Timer.builder("helpflow.tickets.resolution")
                .register(registry).record(elapsed);
    }

    @Override
    public void recordSlaBreached() {
        Counter.builder("helpflow.sla.breaches").register(registry).increment();
    }

    @Override
    public void recordEscalated() {
        Counter.builder("helpflow.tickets.escalated").register(registry).increment();
    }

    @Override
    public void recordDispatchFailure() {
        Counter.builder("helpflow.dispatch.failures").register(registry).increment();
    }

    @Override
    public void recordAuthFailure(String reason) {
        Counter.builder("helpflow.auth.failures")
                .tag("reason", reason)
                .register(registry).increment();
    }

    @Override
    public void recordAccessDenied() {
        Counter.builder("helpflow.access.denied").register(registry).increment();
    }
}
