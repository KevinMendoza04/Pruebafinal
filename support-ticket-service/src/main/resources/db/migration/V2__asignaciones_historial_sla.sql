-- V2: Assignments, ticket history, SLA policies + indexes

CREATE TABLE assignments (
    id            UUID PRIMARY KEY,
    ticket_id     UUID NOT NULL,
    technician_id UUID NOT NULL,
    assigned_at    TIMESTAMPTZ NOT NULL,
    unassigned_at  TIMESTAMPTZ
);

CREATE INDEX idx_assignments_ticket    ON assignments(ticket_id);
CREATE INDEX idx_assignments_technician ON assignments(technician_id);

CREATE TABLE ticket_history (
    id           UUID PRIMARY KEY,
    ticket_id    UUID NOT NULL,
    event_type   VARCHAR(30)  NOT NULL,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20),
    actor_id     UUID,
    observation   VARCHAR(1000),
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_ticket_history_ticket ON ticket_history(ticket_id);

CREATE TABLE sla_policies (
    id               UUID PRIMARY KEY,
    priority         VARCHAR(20) NOT NULL,
    resolution_hours INT NOT NULL,
    CONSTRAINT uk_sla_priority UNIQUE (priority)
);
