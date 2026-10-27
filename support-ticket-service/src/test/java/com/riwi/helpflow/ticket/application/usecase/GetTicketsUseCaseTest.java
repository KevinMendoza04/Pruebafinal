package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.out.*;
import com.riwi.helpflow.ticket.domain.exception.ForbiddenOperationException;
import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTicketsUseCaseTest {

    @Mock TicketRepositoryPort        ticketRepo;
    @Mock TechnicianRepositoryPort    technicianRepo;
    @Mock TicketHistoryRepositoryPort historyRepo;

    GetTicketsUseCaseImpl useCase;

    @BeforeEach void setUp() {
        useCase = new GetTicketsUseCaseImpl(ticketRepo, technicianRepo, historyRepo);
    }

    private Ticket ticket(UUID requesterId, UUID technicianId) {
        return Ticket.reconstitute(UUID.randomUUID(), requesterId,
                "T", "D", TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC", TicketStatus.ASIGNADO, technicianId,
                Instant.now(), Instant.now(), null, null, null, null, null, null);
    }

    @Test @DisplayName("USUARIO can get their own ticket")
    void usuario_getOwnTicket() {
        UUID userId = UUID.randomUUID();
        AuthenticatedActor actor = new AuthenticatedActor(userId, Role.ROLE_USUARIO);
        Ticket t = ticket(userId, null);
        when(ticketRepo.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatNoException().isThrownBy(() -> useCase.getTicketById(actor, t.getId()));
    }

    @Test @DisplayName("USUARIO cannot get another user's ticket — IDOR → 403")
    void usuario_idorDenied() {
        UUID actorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID(); // different user
        AuthenticatedActor actor = new AuthenticatedActor(actorId, Role.ROLE_USUARIO);
        Ticket t = ticket(ownerId, null);
        when(ticketRepo.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> useCase.getTicketById(actor, t.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test @DisplayName("TECNICO can get their assigned ticket")
    void tecnico_getAssignedTicket() {
        UUID techUserId = UUID.randomUUID();
        UUID techId     = UUID.randomUUID();
        AuthenticatedActor actor = new AuthenticatedActor(techUserId, Role.ROLE_TECNICO);

        Technician tech = Technician.create(techId, techUserId, "TEC-001",
                Set.of(TicketCategory.RED), "BOGOTA", "p", TechnicianStatus.ACTIVO, 5);

        Ticket t = ticket(UUID.randomUUID(), techId);
        when(ticketRepo.findById(t.getId())).thenReturn(Optional.of(t));
        when(technicianRepo.findByUserId(techUserId)).thenReturn(Optional.of(tech));

        assertThatNoException().isThrownBy(() -> useCase.getTicketById(actor, t.getId()));
    }

    @Test @DisplayName("TECNICO cannot get ticket assigned to a different technician")
    void tecnico_notAssigned_denied() {
        UUID techUserId = UUID.randomUUID();
        UUID techId     = UUID.randomUUID();
        UUID otherTech  = UUID.randomUUID();
        AuthenticatedActor actor = new AuthenticatedActor(techUserId, Role.ROLE_TECNICO);

        Technician tech = Technician.create(techId, techUserId, "TEC-001",
                Set.of(TicketCategory.RED), "BOGOTA", "p", TechnicianStatus.ACTIVO, 5);

        Ticket t = ticket(UUID.randomUUID(), otherTech); // assigned to a different tech
        when(ticketRepo.findById(t.getId())).thenReturn(Optional.of(t));
        when(technicianRepo.findByUserId(techUserId)).thenReturn(Optional.of(tech));

        assertThatThrownBy(() -> useCase.getTicketById(actor, t.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test @DisplayName("ADMIN can get any ticket")
    void admin_getAnyTicket() {
        AuthenticatedActor admin = new AuthenticatedActor(UUID.randomUUID(), Role.ROLE_ADMIN);
        Ticket t = ticket(UUID.randomUUID(), UUID.randomUUID());
        when(ticketRepo.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatNoException().isThrownBy(() -> useCase.getTicketById(admin, t.getId()));
    }
}
