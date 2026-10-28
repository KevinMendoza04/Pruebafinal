package com.riwi.helpflow.ticket.infrastructure.adapter.out.dispatch;

import com.riwi.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.riwi.helpflow.ticket.domain.exception.DispatchServiceException;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * REST adapter calling technician-dispatch-service.
 * Timeouts: 2s connect, 3s read. No retries per business-rules.md.
 */
@Component
public class RestClientDispatchAdapter implements TechnicianDispatchPort {

    private final RestClient restClient;

    public RestClientDispatchAdapter(@Value("${helpflow.dispatch.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public DispatchSuggestion suggest(long ticketId, TicketCategory category, TicketPriority priority) {
        try {
            DispatchResponse resp = restClient.post()
                    .uri("/technician-dispatch")
                    .header("Content-Type", "application/json")
                    .body(new DispatchRequest(ticketId, category.name(), priority.name()))
                    .retrieve()
                    .body(DispatchResponse.class);

            if (resp == null) throw new DispatchServiceException("Empty dispatch response");
            return new DispatchSuggestion(resp.tecnicoId(), resp.tiempoEstimadoHoras(), resp.disponibilidad());
        } catch (RestClientException ex) {
            throw new DispatchServiceException("Dispatch service unavailable", ex);
        }
    }

    // ── Wire DTOs (Spanish field names per contract) ────────────────────────
    record DispatchRequest(long ticketId, String categoria, String prioridad) {}
    record DispatchResponse(String tecnicoId, int tiempoEstimadoHoras, String disponibilidad) {}
}
