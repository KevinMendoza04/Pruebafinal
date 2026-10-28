package com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.controller;

import com.riwi.helpflow.ticket.application.port.in.ManageTechniciansUseCase;
import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.Technician;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.ActorResolver;
import com.riwi.helpflow.ticket.infrastructure.adapter.in.rest.dto.TechnicianDtos;
import com.riwi.helpflow.ticket.domain.model.TechnicianStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/technicians")
@Tag(name = "Technicians", description = "Technician management endpoints")
public class TechnicianController {

    private final ManageTechniciansUseCase useCase;
    private final ActorResolver            actorResolver;

    public TechnicianController(ManageTechniciansUseCase useCase, ActorResolver actorResolver) {
        this.useCase = useCase;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDtos.TechnicianResponse> create(
            @Valid @RequestBody TechnicianDtos.CreateTechnicianRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Technician t = useCase.createTechnician(actor, new ManageTechniciansUseCase.CreateCommand(
                req.document(), req.fullName(), req.email(), req.site(), req.department(),
                req.code(), req.specialties(), req.supportProfile(),
                req.maxActiveTickets() != null ? req.maxActiveTickets() : 5,
                req.password()));
        return ResponseEntity.ok(toResponse(t));
    }

    @GetMapping
    public ResponseEntity<List<TechnicianDtos.TechnicianResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        return ResponseEntity.ok(useCase.listTechnicians(actor, page, size)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<TechnicianDtos.TechnicianResponse> me(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toResponse(useCase.getMyProfile(actorResolver.resolve(httpRequest))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianDtos.TechnicianResponse> getById(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toResponse(
                useCase.getTechnicianById(actorResolver.resolve(httpRequest), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TechnicianDtos.TechnicianResponse> update(
            @PathVariable UUID id, @Valid @RequestBody TechnicianDtos.UpdateTechnicianRequest req,
            HttpServletRequest httpRequest) {
        AuthenticatedActor actor = actorResolver.resolve(httpRequest);
        Technician t = useCase.updateTechnician(actor, id, new ManageTechniciansUseCase.UpdateCommand(
                req.fullName(), req.email(), req.site(), req.department(),
                req.specialties(), req.supportProfile(),
                req.maxActiveTickets() != null ? req.maxActiveTickets() : 5));
        return ResponseEntity.ok(toResponse(t));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TechnicianDtos.TechnicianResponse> changeStatus(
            @PathVariable UUID id, @RequestBody TechnicianStatus status,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toResponse(
                useCase.changeTechnicianStatus(actorResolver.resolve(httpRequest), id, status)));
    }

    private TechnicianDtos.TechnicianResponse toResponse(Technician t) {
        return new TechnicianDtos.TechnicianResponse(t.getId(), t.getCode(),
                t.getSpecialties(), t.getSite(), t.getSupportProfile(),
                t.getStatus(), t.getMaxActiveTickets(), t.getUserId());
    }
}
