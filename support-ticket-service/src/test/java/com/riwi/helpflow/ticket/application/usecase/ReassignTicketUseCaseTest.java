package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.model.*;
import com.riwi.helpflow.ticket.domain.service.TechnicianSelectionPolicy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReassignTicketUseCaseTest {

    @Mock TicketRepositoryPort        ticketRepo;
    @Mock TechnicianRepositoryPort    technicianRepo;
    @Mock AssignmentRepositoryPort    assignmentRepo;
    @Mock TicketHistoryRepositoryPort historyRepo;

    ReassignTicketUseCaseImpl useCase;
    Clock                     clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    AuthenticatedActor        admin = new AuthenticatedActor(UUID.randomUUID(), Role.ROLE_ADMIN);

    @BeforeEach void setUp() {
        useCase = new ReassignTicketUseCaseImpl(ticketRepo, technicianRepo, assignmentRepo,
                historyRepo, new TechnicianSelectionPolicy(), clock);
    }

    private Ticket asignadoTicket() {
        return Ticket.reconstitute(UUID.randomUUID(), UUID.randomUUID(),
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC", TicketStatus.ASIGNADO, UUID.randomUUID(),
                Instant.now(), Instant.now(), Instant.now().plusSeconds(14400),
                null, null, null, null, null);
    }

    private Technician eligibleTech(UUID id) {
        Technician t = Technician.create(id, UUID.randomUUID(), "TEC-001",
                Set.of(TicketCategory.RED), "BOGOTA", "profile",
                TechnicianStatus.ACTIVO, 5);
        t.setActiveTicketCount(0);
        return t;
    }

    @Test @DisplayName("Non-admin cannot reassign — throws ForbiddenOperationException")
    void nonAdmin_throws() {
        AuthenticatedActor user = new AuthenticatedActor(UUID.randomUUID(), Role.ROLE_USUARIO);
        assertThatThrownBy(() -> useCase.reassign(user, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test @DisplayName("Successful reassignment creates new assignment and REASSIGNED history")
    void reassign_success() {
        Ticket ticket = asignadoTicket();
        UUID newTechId = UUID.randomUUID();
        Technician newTech = eligibleTech(newTechId);

        when(ticketRepo.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(technicianRepo.lockForUpdate(newTechId)).thenReturn(newTech);
        when(assignmentRepo.findActiveByTicketId(ticket.getId())).thenReturn(Optional.empty());
        when(assignmentRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(ticketRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(historyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Ticket result = useCase.reassign(admin, ticket.getId(), newTechId);

        assertThat(result.getAssignedTechnicianId()).isEqualTo(newTechId);
        verify(historyRepo).save(argThat(h ->
                h.getEventType() == HistoryEventType.REASSIGNED));
    }

    @Test @DisplayName("Ineligible technician (wrong site, ALTA priority) throws")
    void ineligibleTech_throws() {
        Ticket ticket = asignadoTicket();  // priority ALTA, site BOGOTA
        UUID techId = UUID.randomUUID();
        Technician wrongSite = Technician.create(techId, UUID.randomUUID(), "TEC-002",
                Set.of(TicketCategory.RED), "MEDELLIN", "p",
                TechnicianStatus.ACTIVO, 5);
        wrongSite.setActiveTicketCount(0);

        when(ticketRepo.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(technicianRepo.lockForUpdate(techId)).thenReturn(wrongSite);

        assertThatThrownBy(() -> useCase.reassign(admin, ticket.getId(), techId))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("elegibilidad");
    }
}
