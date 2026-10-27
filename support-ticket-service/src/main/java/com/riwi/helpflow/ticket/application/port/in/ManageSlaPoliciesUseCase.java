package com.riwi.helpflow.ticket.application.port.in;

import com.riwi.helpflow.ticket.application.usecase.AuthenticatedActor;
import com.riwi.helpflow.ticket.domain.model.SlaPolicy;
import com.riwi.helpflow.ticket.domain.model.TicketPriority;

import java.util.List;
import java.util.Map;

public interface ManageSlaPoliciesUseCase {
    List<SlaPolicy> getPolicies(AuthenticatedActor actor);
    List<SlaPolicy> updatePolicies(AuthenticatedActor actor, Map<TicketPriority, Integer> newHours);
}
