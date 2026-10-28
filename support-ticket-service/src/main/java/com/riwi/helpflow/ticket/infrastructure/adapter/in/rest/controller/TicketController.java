package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.controller;

import com.riwi.helpflow.ticket.application.port.in.*;
import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.domain.model.TicketCategory;
import com.riwi.helpflow.ticket.domain.model.TicketHistoryEntry;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.model.TicketStatus;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.ActorResolver;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.TicketDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Tickets", description = "Ticket management endpoints")
public class TicketController {

    private final CreateTicketUseCase      createUseCase;
    private final GetTicketsUseCase         getUseCase;
    private final UpdateTicketUseCase       updateUseCase;
    private final TransitionTicketUseCase   transitionUseCase;
    private final CancelTicketUseCase       cancelUseCase;
    private final ActorResolver             actorResolver;

    public TicketController(CreateTicketUseCase createUseCase,
                             GetTicketsUseCase getUseCase,
                             UpdateTicketUseCase updateUseCase,
                             TransitionTicketUseCase transitionUseCase,
                             CancelTicketUseCase cancelUseCase,
                             ActorResolver actorResolver) {
        this.createUseCase    = createUseCase;
        this.getUseCase       = getUseCase;
        this.updateUseCase    = updateUseCase;
        this.transitionUseCase = transitionUseCase;
        this.cancelUseCase    = cancelUseCase;
        this.actorResolver    = actorResolver;
    }

    @PostMapping
    public ResponseEntity<TicketDtos.TicketResponse> create(
            @Valid @RequestBody TicketDtos.CreateTicketRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = createUseCase.create(actor, new CreateTicketUseCase.Command(
                req.title(), req.description(), req.category(),
                req.priority(), req.site(), req.affectedAsset()));
        return ResponseEntity.ok(toResponse(ticket));
    }

    @GetMapping
    public ResponseEntity<List<TicketDtos.TicketResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        List<Ticket> tickets = getUseCase.listTickets(actor, page, size, status, priority, category);
        return ResponseEntity.ok(tickets.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDtos.TicketResponse> getById(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = getUseCase.getTicketById(actor, id);
        return ResponseEntity.ok(toResponse(ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDtos.TicketResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TicketDtos.UpdateTicketRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = updateUseCase.update(actor, id, new UpdateTicketUseCase.Command(
                req.title(), req.description(), req.affectedAsset()));
        return ResponseEntity.ok(toResponse(ticket));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TicketDtos.TicketResponse> cancel(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Cancelado por el solicitante")
            String observation,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = cancelUseCase.cancel(actor, id, observation);
        return ResponseEntity.ok(toResponse(ticket));
    }

    @PostMapping("/{id}/transitions")
    public ResponseEntity<TicketDtos.TicketResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TicketDtos.TransitionRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = transitionUseCase.transition(actor, id,
                new TransitionTicketUseCase.Command(req.targetStatus(), req.observation()));
        return ResponseEntity.ok(toResponse(ticket));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketDtos.TicketHistoryResponse>> history(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        List<TicketHistoryEntry> entries = getUseCase.getHistory(actor, id);
        return ResponseEntity.ok(entries.stream().map(this::toHistoryResponse).toList());
    }

    // ── Mappers ────────────────────────────────────────────────────────────
    private TicketDtos.TicketResponse toResponse(Ticket t) {
        return new TicketDtos.TicketResponse(t.getId(), t.getRequesterId(),
                t.getTitle(), t.getDescription(), t.getCategory(), t.getPriority(),
                t.getSite(), t.getAffectedAsset(), t.getStatus(),
                t.getAssignedTechnicianId(), t.getCreatedAt(), t.getSlaDeadline(),
                t.getFirstRespondedAt(), t.getResolvedAt(), t.getEscalatedAt());
    }

    private TicketDtos.TicketHistoryResponse toHistoryResponse(TicketHistoryEntry h) {
        return new TicketDtos.TicketHistoryResponse(h.getId(),
                h.getEventType().name(),
                h.getFromStatus() != null ? h.getFromStatus().name() : null,
                h.getToStatus()   != null ? h.getToStatus().name()   : null,
                h.getActorId(), h.getObservation(), h.getCreatedAt());
    }
}
