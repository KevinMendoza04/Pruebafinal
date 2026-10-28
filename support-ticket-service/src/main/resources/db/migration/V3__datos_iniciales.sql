-- V3: Default SLA policies only (no users, no passwords, no technicians)
-- Per business-rules.md: CRITICA 2h, ALTA 4h, MEDIA 8h, BAJA 24h

INSERT INTO sla_policies (id, priority, resolution_hours) VALUES
    (gen_random_uuid(), 'CRITICA', 2),
    (gen_random_uuid(), 'ALTA',    4),
    (gen_random_uuid(), 'MEDIA',   8),
    (gen_random_uuid(), 'BAJA',    24);
