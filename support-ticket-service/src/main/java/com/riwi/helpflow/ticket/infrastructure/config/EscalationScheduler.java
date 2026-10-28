package com.riwi.helpflow.ticket.infrastructure.config;

import com.riwi.helpflow.ticket.application.port.in.EscalateTicketsUseCase;
import com.riwi.helpflow.ticket.infrastructure.adapter.out.metrics.MicrometerTicketMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled escalation scan — runs the EscalateTicketsUseCase periodically.
 * The use case is testable independently; this scheduler only triggers it.
 */
@Component
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalateTicketsUseCase escalateUseCase;
    private final MicrometerTicketMetricsAdapter metricsAdapter;

    public EscalationScheduler(EscalateTicketsUseCase escalateUseCase,
                                MicrometerTicketMetricsAdapter metricsAdapter) {
        this.escalateUseCase = escalateUseCase;
        this.metricsAdapter = metricsAdapter;
    }

    @Scheduled(fixedDelayString = "${helpflow.escalation.scan-interval:60s}")
    public void runEscalation() {
        int escalated = escalateUseCase.escalate();
        metricsAdapter.refreshOverdueGauge();
        if (escalated > 0) {
            log.info("Escalation scan completed: {} tickets escalated", escalated);
        }
    }
}
