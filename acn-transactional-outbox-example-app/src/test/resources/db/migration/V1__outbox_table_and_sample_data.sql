CREATE TABLE IF NOT EXISTS transactional_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    creation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    last_attempt_date TIMESTAMP WITH TIME ZONE,
    completion_date TIMESTAMP WITH TIME ZONE,
    attempts INT NOT NULL,
    event TEXT NOT NULL,
    last_error TEXT
);

INSERT INTO transactional_outbox (id, event_type, creation_date, last_attempt_date, completion_date, attempts, event, last_error)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'EVENT_TYPE_1', now() - interval '1 day', now() - interval '1 day', now() - interval '1 day', 1, 'EVENT_DATA_1', NULL),
    ('22222222-2222-2222-2222-222222222222', 'EVENT_TYPE_2', now() - interval '2 days', now() - interval '5 minutes', NULL, 2, 'EVENT_DATA_2', 'Temporary error'),
    ('33333333-3333-3333-3333-333333333333', 'EVENT_TYPE_3', now() - interval '3 days', now() - interval '125 minutes', NULL, 3, 'EVENT_DATA_3', 'Temporary error'),
    ('44444444-4444-4444-4444-444444444444', 'EVENT_TYPE_4', now() - interval '4 days', now() - interval '1 day', now() - interval '1 day', 1, 'EVENT_DATA_4', NULL),
    ('55555555-5555-5555-5555-555555555555', 'EVENT_TYPE_5', now() - interval '5 days', now() - interval '5 minutes', now() - interval '5 minutes', 3, 'EVENT_DATA_5', 'Temporary error'),
    ('66666666-6666-6666-6666-666666666666', 'EVENT_TYPE_6', now() - interval '6 days', now() - interval '25 minutes', NULL, 2, 'EVENT_DATA_6', 'Temporary error'),
    ('77777777-7777-7777-7777-777777777777', 'EVENT_TYPE_7', now() - interval '7 days', now() - interval '2 hours', now() - interval '2 hours', 1, 'EVENT_DATA_7', NULL),
    ('88888888-8888-8888-8888-888888888888', 'EVENT_TYPE_8', now() - interval '8 days', now() - interval '125 minutes', NULL, 3, 'EVENT_DATA_8', 'Temporary error'),
    ('99999999-9999-9999-9999-999999999999', 'EVENT_TYPE_9', now() - interval '9 days', now() - interval '125 minutes', now() - interval '1 hour', 3, 'EVENT_DATA_9', 'Temporary error'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'EVENT_TYPE_10', now() - interval '10 days', now() - interval '6 hours', now() - interval '6 hours', 1, 'EVENT_DATA_10', NULL);
