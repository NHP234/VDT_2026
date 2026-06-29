INSERT INTO agents (id, email, display_name, password_hash, active, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'agent@example.test', 'Demo Agent', '$2a$12$tPEMwfLM6K65Qvgk3V.bzeRjjL2apyYqRQBgP8FVzIJwpzPJe8eTe', TRUE, '2026-06-22T00:00:00Z'),
    ('10000000-0000-0000-0000-000000000002', 'minh.agent@example.test', 'Minh Agent', '$2a$12$tPEMwfLM6K65Qvgk3V.bzeRjjL2apyYqRQBgP8FVzIJwpzPJe8eTe', TRUE, '2026-06-22T00:00:00Z'),
    ('10000000-0000-0000-0000-000000000003', 'linh.agent@example.test', 'Linh Agent', '$2a$12$tPEMwfLM6K65Qvgk3V.bzeRjjL2apyYqRQBgP8FVzIJwpzPJe8eTe', TRUE, '2026-06-22T00:00:00Z');

INSERT INTO customers (id, display_name, created_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'Nguyen Van A', '2026-06-22T00:00:00Z'),
    ('20000000-0000-0000-0000-000000000002', 'Tran Thi B', '2026-06-22T00:00:00Z');

INSERT INTO channel_identities (
    id, customer_id, channel, provider_account_id, external_identity_id, display_name, created_at
)
VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'FACEBOOK', 'local-page-id', 'fb-user-a', 'Nguyen Van A', '2026-06-22T00:00:00Z'),
    ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'EMAIL', 'demo@example.test', 'tran.b@example.test', 'Tran Thi B', '2026-06-22T00:00:00Z');

INSERT INTO conversations (
    id, customer_id, channel_identity_id, channel, source_type, status, assigned_agent_id,
    provider_account_id, external_conversation_id, subject, last_message_preview,
    last_activity_at, created_at, updated_at
)
VALUES
    (
        '40000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        'FACEBOOK',
        'MESSAGE',
        'OPEN',
        NULL,
        'local-page-id',
        'messenger:fb-user-a',
        NULL,
        'Shop oi don hang cua minh den dau roi?',
        '2026-06-22T02:00:00Z',
        '2026-06-22T02:00:00Z',
        '2026-06-22T02:00:00Z'
    ),
    (
        '40000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        '30000000-0000-0000-0000-000000000002',
        'EMAIL',
        'EMAIL',
        'PENDING',
        '10000000-0000-0000-0000-000000000001',
        'demo@example.test',
        'email:thread:demo-order-42',
        'Can ho tro don hang',
        'Minh can ho tro ve don hang #42',
        '2026-06-22T03:00:00Z',
        '2026-06-22T03:00:00Z',
        '2026-06-22T03:00:00Z'
    );

INSERT INTO messages (
    id, conversation_id, channel, direction, delivery_status, provider_account_id,
    external_message_id, content, occurred_at, created_at
)
VALUES
    (
        '50000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001',
        'FACEBOOK',
        'INBOUND',
        'RECEIVED',
        'local-page-id',
        'mid.local.facebook.1',
        'Shop oi don hang cua minh den dau roi?',
        '2026-06-22T02:00:00Z',
        '2026-06-22T02:00:00Z'
    ),
    (
        '50000000-0000-0000-0000-000000000002',
        '40000000-0000-0000-0000-000000000002',
        'EMAIL',
        'INBOUND',
        'RECEIVED',
        'demo@example.test',
        '<demo-order-42@example.test>',
        'Minh can ho tro ve don hang #42',
        '2026-06-22T03:00:00Z',
        '2026-06-22T03:00:00Z'
    );

INSERT INTO conversation_activities (
    id, conversation_id, actor_agent_id, activity_type, old_value, new_value, created_at
)
VALUES
    (
        '60000000-0000-0000-0000-000000000001',
        '40000000-0000-0000-0000-000000000001',
        NULL,
        'MESSAGE_RECEIVED',
        NULL,
        'mid.local.facebook.1',
        '2026-06-22T02:00:00Z'
    ),
    (
        '60000000-0000-0000-0000-000000000002',
        '40000000-0000-0000-0000-000000000002',
        '10000000-0000-0000-0000-000000000001',
        'ASSIGNMENT_CHANGED',
        'Unassigned',
        'agent@example.test',
        '2026-06-22T03:10:00Z'
    );
