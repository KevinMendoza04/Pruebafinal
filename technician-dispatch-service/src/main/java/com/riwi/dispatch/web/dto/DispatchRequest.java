package com.riwi.dispatch.web.dto;

import com.riwi.dispatch.domain.TicketCategory;
import com.riwi.dispatch.domain.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for POST /technician-dispatch.
 * Field names are Spanish to match the exact JSON contract in business-rules.md.
 */
@Schema(description = "Dispatch request payload")
public record DispatchRequest(

        @Schema(description = "Ticket identifier", example = "125")
        @NotNull(message = "ticketId es obligatorio")
        @Positive(message = "ticketId debe ser mayor que cero")
        Long ticketId,

        @Schema(description = "Ticket category", example = "RED",
                allowableValues = {"HARDWARE","SOFTWARE","RED","ACCESOS","OTRO"})
        @NotNull(message = "categoria es obligatoria")
        TicketCategory categoria,

        @Schema(description = "Ticket priority", example = "ALTA",
                allowableValues = {"BAJA","MEDIA","ALTA","CRITICA"})
        @NotNull(message = "prioridad es obligatoria")
        TicketPriority prioridad

) {}
