package com.riwi.helpflow.ticket.domain.model;

import java.util.Set;

public enum TicketStatus {
    ABIERTO, ASIGNADO, EN_PROGRESO, RESUELTO, CERRADO, CANCELADO;

    private static final Set<TicketStatus> TERMINAL = Set.of(CERRADO, CANCELADO);
    private static final Set<TicketStatus> OPEN_STATUSES =
            Set.of(ABIERTO, ASIGNADO, EN_PROGRESO);

    public boolean isTerminal() { return TERMINAL.contains(this); }
    public boolean isOpen()     { return OPEN_STATUSES.contains(this); }
}
