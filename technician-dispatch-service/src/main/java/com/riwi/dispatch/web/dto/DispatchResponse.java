package com.riwi.dispatch.web.dto;

import com.riwi.dispatch.domain.Availability;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for POST /technician-dispatch.
 * Field names are Spanish to match the exact JSON contract in business-rules.md.
 * Example: {"ticketId":125,"tecnicoId":"TEC-014","disponibilidad":"MEDIA","tiempoEstimadoHoras":4}
 */
@Schema(description = "Dispatch response payload")
public record DispatchResponse(

        @Schema(description = "Ticket identifier echoed back", example = "125")
        Long ticketId,

        @Schema(description = "Suggested technician code", example = "TEC-014")
        String tecnicoId,

        @Schema(description = "Technician availability", example = "MEDIA")
        Availability disponibilidad,

        @Schema(description = "Estimated attention time in hours", example = "4")
        int tiempoEstimadoHoras

) {}
