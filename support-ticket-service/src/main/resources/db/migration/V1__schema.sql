-- V1: Core schema — users, technicians, technician_specialties, tickets
-- Constraints: unique document, email, technician code, technician user_id
-- Indexes: tickets(status), tickets(sla_deadline), tickets(requester_id), tickets(assigned_technician_id)

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    document      VARCHAR(50)  NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    site          VARCHAR(100),
    department    VARCHAR(100),
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT uk_users_document UNIQUE (document),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

CREATE TABLE technicians (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL,
    code              VARCHAR(20)  NOT NULL,
    site              VARCHAR(100),
    support_profile   VARCHAR(1000),
    status            VARCHAR(20)  NOT NULL,
    max_active_tickets INT NOT NULL DEFAULT 5,
    CONSTRAINT uk_technicians_code    UNIQUE (code),
    CONSTRAINT uk_technicians_user_id UNIQUE (user_id)
);

CREATE TABLE technician_specialties (
    technician_id UUID NOT NULL,
    specialty     VARCHAR(20) NOT NULL,
    PRIMARY KEY (technician_id, specialty),
    CONSTRAINT fk_tech_spec_tech FOREIGN KEY (technician_id) REFERENCES technicians(id) ON DELETE CASCADE
);

CREATE TABLE tickets (
    id                     UUID PRIMARY KEY,
    requester_id           UUID NOT NULL,
    title                  VARCHAR(120)  NOT NULL,
    description            VARCHAR(2000) NOT NULL,
    category               VARCHAR(20)   NOT NULL,
    priority               VARCHAR(20)   NOT NULL,
    site                   VARCHAR(100),
    affected_asset         VARCHAR(100),
    status                 VARCHAR(20)   NOT NULL,
    assigned_technician_id UUID,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ,
    sla_deadline           TIMESTAMPTZ,
    first_responded_at     TIMESTAMPTZ,
    resolved_at            TIMESTAMPTZ,
    escalated_at           TIMESTAMPTZ,
    sla_breached_at        TIMESTAMPTZ,
    version                BIGINT
);

CREATE INDEX idx_tickets_status        ON tickets(status);
CREATE INDEX idx_tickets_sla_deadline  ON tickets(sla_deadline);
CREATE INDEX idx_tickets_requester     ON tickets(requester_id);
CREATE INDEX idx_tickets_technician    ON tickets(assigned_technician_id);
