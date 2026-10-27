package com.riwi.helpflow.ticket.application.port.out;

import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

/**
 * Outbound port for the external technician dispatch service.
 * The infrastructure adapter implements this using RestClient.
 */
public interface TechnicianDispatchPort {

    /**
     * Asks the dispatch service to suggest a technician for the given ticket context.
     *
     * @throws com.riwi.helpflow.ticket.domain.exception.DispatchServiceException on timeout or HTTP error
     */
    DispatchSuggestion suggest(long ticketId, TicketCategory category, TicketPriority priority);

    record DispatchSuggestion(
            String technicianCode,    // e.g. "TEC-014"
            int    estimatedHours,
            String availability       // ALTA / MEDIA / BAJA
    ) {}
}
