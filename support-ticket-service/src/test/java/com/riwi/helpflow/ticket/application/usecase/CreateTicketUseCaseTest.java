package com.riwi.helpflow.ticket.application.usecase;

import com.riwi.helpflow.ticket.application.port.in.CreateTicketUseCase;
import com.riwi.helpflow.ticket.application.port.out.TechnicianDispatchPort;
import com.riwi.helpflow.ticket.application.port.out.TicketMetricsPort;
import com.riwi.helpflow.ticket.domain.exception.DispatchServiceException;
import com.riwi.helpflow.ticket.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class CreateTicketUseCaseTest {

    @Mock CreateTicketTxPort     createTx;
    @Mock AssignTicketTxPort     assignTx;
    @Mock TechnicianDispatchPort dispatch;
    @Mock TicketMetricsPort      metrics;

    CreateTicketUseCaseImpl useCase;
    AuthenticatedActor      actor;

    @BeforeEach void setUp() {
        useCase = new CreateTicketUseCaseImpl(createTx, assignTx, dispatch, metrics);
        actor   = new AuthenticatedActor(UUID.randomUUID(), Role.ROLE_USUARIO);
    }

    private Ticket sampleTicket(TicketStatus status) {
        return Ticket.reconstitute(UUID.randomUUID(), actor.userId(),
                "Title", "Description long enough",
                TicketCategory.RED, TicketPriority.ALTA,
                "BOGOTA", "PC-01", status, null,
                Instant.now(), Instant.now(), Instant.now().plusSeconds(14400),
                null, null, null, null, null);
    }

    @Test @DisplayName("Dispatch accepted: assignTx called with suggestion")
    void dispatchAccepted_assignWithSuggestion() {
        Ticket created  = sampleTicket(TicketStatus.ABIERTO);
        Ticket assigned = sampleTicket(TicketStatus.ASIGNADO);
        TechnicianDispatchPort.DispatchSuggestion suggestion =
                new TechnicianDispatchPort.DispatchSuggestion("TEC-003", 2, "ALTA");

        when(createTx.createAndCommit(eq(actor), any())).thenReturn(created);
        when(dispatch.suggest(anyLong(), any(), any())).thenReturn(suggestion);
        when(assignTx.assignAndCommit(eq(created.getId()), eq(suggestion))).thenReturn(assigned);

        CreateTicketUseCase.Command cmd = new CreateTicketUseCase.Command(
                "Title", "Description long enough",
                TicketCategory.RED, TicketPriority.ALTA, "BOGOTA", "PC-01");

        Ticket result = useCase.create(actor, cmd);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.ASIGNADO);
        verify(metrics).ticketCreated(TicketCategory.RED, TicketPriority.ALTA);
        verify(assignTx).assignAndCommit(eq(created.getId()), eq(suggestion));
    }

    @Test @DisplayName("Dispatch failure: assignTx called with null suggestion, ticket stays ABIERTO")
    void dispatchFailure_ticketStaysAbierto() {
        Ticket created = sampleTicket(TicketStatus.ABIERTO);

        when(createTx.createAndCommit(eq(actor), any())).thenReturn(created);
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenThrow(new DispatchServiceException("timeout"));
        when(assignTx.assignAndCommit(eq(created.getId()), isNull())).thenReturn(created);

        CreateTicketUseCase.Command cmd = new CreateTicketUseCase.Command(
                "Title", "Description long enough",
                TicketCategory.RED, TicketPriority.ALTA, "BOGOTA", "PC-01");

        Ticket result = useCase.create(actor, cmd);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.ABIERTO);
        verify(metrics).recordDispatchFailure();
        verify(assignTx).assignAndCommit(eq(created.getId()), isNull());
    }

    @Test @DisplayName("Metrics counter incremented for every created ticket")
    void metricsCounterIncremented() {
        Ticket created = sampleTicket(TicketStatus.ABIERTO);
        when(createTx.createAndCommit(eq(actor), any())).thenReturn(created);
        when(dispatch.suggest(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("unavailable"));
        when(assignTx.assignAndCommit(any(), any())).thenReturn(created);

        CreateTicketUseCase.Command cmd = new CreateTicketUseCase.Command(
                "Title", "Description long enough",
                TicketCategory.RED, TicketPriority.ALTA, "BOGOTA", "PC-01");
        useCase.create(actor, cmd);
        verify(metrics, times(1)).ticketCreated(TicketCategory.RED, TicketPriority.ALTA);
    }
}
