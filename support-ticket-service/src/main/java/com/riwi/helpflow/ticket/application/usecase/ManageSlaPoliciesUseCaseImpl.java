package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.ManageSlaPoliciesUseCase;
import com.riwi.helpflow.ticket.application.port.out.SlaPolicyRepositoryPort;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;
import com.riwi.helpflow.ticket.domain.service.SlaCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public class ManageSlaPoliciesUseCaseImpl implements ManageSlaPoliciesUseCase {

    private final SlaPolicyRepositoryPort policyRepo;
    private final SlaCalculator           slaCalculator;

    public ManageSlaPoliciesUseCaseImpl(SlaPolicyRepositoryPort policyRepo,
                                        SlaCalculator slaCalculator) {
        this.policyRepo   = policyRepo;
        this.slaCalculator = slaCalculator;
    }

    @Override
    public List<SlaPolicy> getPolicies(AuthenticatedActor actor) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");
        return policyRepo.findAll();
    }

    @Override
    @Transactional
    public List<SlaPolicy> updatePolicies(AuthenticatedActor actor,
                                          Map<TicketPriority, Integer> newHours) {
        if (!actor.isAdmin()) throw new ForbiddenOperationException("Solo administradores");

        // Cross-validate: CRITICA < ALTA < MEDIA < BAJA
        slaCalculator.validatePolicies(newHours);

        for (Map.Entry<TicketPriority, Integer> entry : newHours.entrySet()) {
            policyRepo.findByPriority(entry.getKey()).ifPresent(p -> {
                p.updateHours(entry.getValue());
                policyRepo.save(p);
            });
        }
        return policyRepo.findAll();
    }
}
