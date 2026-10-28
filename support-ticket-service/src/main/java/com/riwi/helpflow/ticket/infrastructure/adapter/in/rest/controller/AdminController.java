package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.controller;

import com.riwi.helpflow.ticket.application.port.in.EscalateTicketsUseCase;
import com.riwi.helpflow.ticket.application.port.in.ManageSlaPoliciesUseCase;
import com.riwi.helpflow.ticket.application.port.in.ReassignTicketUseCase;
import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.Ticket;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.ActorResolver;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.AdminDtos;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.TicketDtos;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative endpoints")
public class AdminController {

    private final ReassignTicketUseCase     reassignUseCase;
    private final ManageSlaPoliciesUseCase   slaUseCase;
    private final EscalateTicketsUseCase     escalateUseCase;
    private final ActorResolver             actorResolver;

    public AdminController(ReassignTicketUseCase reassignUseCase,
                            ManageSlaPoliciesUseCase slaUseCase,
                            EscalateTicketsUseCase escalateUseCase,
                            ActorResolver actorResolver) {
        this.reassignUseCase  = reassignUseCase;
        this.slaUseCase       = slaUseCase;
        this.escalateUseCase  = escalateUseCase;
        this.actorResolver    = actorResolver;
    }

    @PutMapping("/tickets/{id}/reassign")
    public ResponseEntity<TicketDtos.TicketResponse> reassign(
            @PathVariable UUID id,
            @Valid @RequestBody AdminDtos.ReassignRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Ticket ticket = reassignUseCase.reassign(actor, id, req.newTechnicianId());
        return ResponseEntity.ok(toResponse(ticket));
    }

    @GetMapping("/sla-policies")
    public ResponseEntity<List<AdminDtos.SlaPolicyResponse>> getSlaPolicies(HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        List<SlaPolicy> policies = slaUseCase.getPolicies(actor);
        return ResponseEntity.ok(policies.stream().map(this::toSlaResponse).toList());
    }

    @PutMapping("/sla-policies")
    public ResponseEntity<List<AdminDtos.SlaPolicyResponse>> updateSlaPolicies(
            @Valid @RequestBody AdminDtos.SlaPoliciesRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        List<SlaPolicy> policies = slaUseCase.updatePolicies(actor, req.policies());
        return ResponseEntity.ok(policies.stream().map(this::toSlaResponse).toList());
    }

    @PostMapping("/escalation/run")
    public ResponseEntity<String> runEscalation() {
        int count = escalateUseCase.escalate();
        return ResponseEntity.ok("Tickets escalated: " + count);
    }

    private TicketDtos.TicketResponse toResponse(Ticket t) {
        return new TicketDtos.TicketResponse(t.getId(), t.getRequesterId(),
                t.getTitle(), t.getDescription(), t.getCategory(), t.getPriority(),
                t.getSite(), t.getAffectedAsset(), t.getStatus(),
                t.getAssignedTechnicianId(), t.getCreatedAt(), t.getSlaDeadline(),
                t.getFirstRespondedAt(), t.getResolvedAt(), t.getEscalatedAt());
    }

    private AdminDtos.SlaPolicyResponse toSlaResponse(SlaPolicy p) {
        return new AdminDtos.SlaPolicyResponse(p.getId(), p.getPriority(), p.getResolutionHours());
    }
}
