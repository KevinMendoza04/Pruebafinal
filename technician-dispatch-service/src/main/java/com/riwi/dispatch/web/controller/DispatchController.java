package com.riwi.dispatch.web.controller;

import com.riwi.dispatch.domain.DispatchResult;
import com.riwi.dispatch.domain.TechnicianDispatcher;
import com.riwi.dispatch.web.dto.DispatchRequest;
import com.riwi.dispatch.web.dto.DispatchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the single dispatch endpoint used by support-ticket-service.
 * No security — this is a simulated internal service.
 */
@RestController
@RequestMapping("/technician-dispatch")
@Tag(name = "Dispatch", description = "Deterministic technician suggestion")
public class DispatchController {

    private final TechnicianDispatcher dispatcher;

    // Constructor injection (no field @Autowired)
    public DispatchController(TechnicianDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Suggest a technician for a ticket",
        description = "Returns a deterministic technician and estimated attention time based on CRC32 of ticketId|categoria|prioridad",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                examples = @ExampleObject(
                    name = "example",
                    value = """
                            {"ticketId":125,"categoria":"RED","prioridad":"ALTA"}
                            """
                )
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Technician suggested",
                content = @Content(schema = @Schema(implementation = DispatchResponse.class),
                    examples = @ExampleObject(
                        value = """
                                {"ticketId":125,"tecnicoId":"TEC-014","disponibilidad":"MEDIA","tiempoEstimadoHoras":4}
                                """
                    )
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request body or unknown enum value")
        }
    )
    public DispatchResponse dispatch(@Valid @RequestBody DispatchRequest request) {
        DispatchResult result = dispatcher.dispatch(
                request.ticketId(),
                request.categoria(),
                request.prioridad()
        );

        return new DispatchResponse(
                request.ticketId(),
                result.technicianCode(),
                result.availability(),
                result.estimatedHours()
        );
    }
}
