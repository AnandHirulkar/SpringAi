-- Demo data only. Knowledge-base documents are ingested through the API because they require
-- embeddings; nothing here depends on an external service.

INSERT INTO customers (name, email, company, tier, created_at)
VALUES ('Ada Lovelace', 'ada@acme.test', 'Acme Analytics', 'ENTERPRISE', UTC_TIMESTAMP(6)),
       ('Grace Hopper', 'grace@globex.test', 'Globex', 'PREMIUM', UTC_TIMESTAMP(6));

INSERT INTO tickets (reference, customer_id, subject, description, status, priority, assigned_agent,
                     version, created_at, updated_at)
SELECT 'TKT-DEMO0001',
       id,
       'CSV export fails for reports over 10k rows',
       'Every export above 10,000 rows returns HTTP 500. Smaller exports work. This blocks our month-end reporting.',
       'OPEN',
       'HIGH',
       'nova',
       0,
       UTC_TIMESTAMP(6),
       UTC_TIMESTAMP(6)
FROM customers
WHERE email = 'ada@acme.test';

INSERT INTO ticket_messages (ticket_id, author_type, author_name, body, created_at)
SELECT id,
       'CUSTOMER',
       'Ada Lovelace',
       'Reproduced again today. The job fails after about 30 seconds with a 500 response.',
       UTC_TIMESTAMP(6)
FROM tickets
WHERE reference = 'TKT-DEMO0001';
