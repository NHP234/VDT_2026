CREATE TABLE agents (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_agents_email ON agents (LOWER(email));

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE channel_identities (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    channel VARCHAR(20) NOT NULL,
    provider_account_id VARCHAR(200) NOT NULL,
    external_identity_id VARCHAR(300) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_channel_identities_channel CHECK (channel IN ('FACEBOOK', 'EMAIL')),
    CONSTRAINT ux_channel_identities_external UNIQUE (channel, provider_account_id, external_identity_id)
);

CREATE INDEX ix_channel_identities_customer_id ON channel_identities (customer_id);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers (id),
    channel_identity_id UUID NOT NULL REFERENCES channel_identities (id),
    channel VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_agent_id UUID REFERENCES agents (id),
    provider_account_id VARCHAR(200) NOT NULL,
    external_conversation_id VARCHAR(500) NOT NULL,
    subject VARCHAR(300),
    last_message_preview VARCHAR(500) NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_conversations_channel CHECK (channel IN ('FACEBOOK', 'EMAIL')),
    CONSTRAINT ck_conversations_source_type CHECK (source_type IN ('MESSAGE', 'COMMENT', 'EMAIL')),
    CONSTRAINT ck_conversations_status CHECK (status IN ('OPEN', 'PENDING', 'RESOLVED')),
    CONSTRAINT ux_conversations_external UNIQUE (channel, provider_account_id, source_type, external_conversation_id)
);

CREATE INDEX ix_conversations_status ON conversations (status);
CREATE INDEX ix_conversations_channel ON conversations (channel);
CREATE INDEX ix_conversations_assigned_agent_id ON conversations (assigned_agent_id);
CREATE INDEX ix_conversations_last_activity_at ON conversations (last_activity_at DESC);
CREATE INDEX ix_conversations_customer_id ON conversations (customer_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    channel VARCHAR(20) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    provider_account_id VARCHAR(200) NOT NULL,
    external_message_id VARCHAR(500),
    content VARCHAR(10000) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_messages_channel CHECK (channel IN ('FACEBOOK', 'EMAIL')),
    CONSTRAINT ck_messages_direction CHECK (direction IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT ck_messages_delivery_status CHECK (delivery_status IN ('RECEIVED', 'QUEUED', 'SENT', 'FAILED'))
);

CREATE INDEX ix_messages_conversation_time ON messages (conversation_id, occurred_at ASC);
CREATE UNIQUE INDEX ux_messages_external_message
    ON messages (channel, provider_account_id, external_message_id)
    WHERE external_message_id IS NOT NULL;

CREATE TABLE conversation_activities (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    actor_agent_id UUID REFERENCES agents (id),
    activity_type VARCHAR(40) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_conversation_activities_type CHECK (
        activity_type IN ('STATUS_CHANGED', 'ASSIGNMENT_CHANGED', 'MESSAGE_RECEIVED', 'REPLY_QUEUED', 'DELIVERY_STATUS_CHANGED')
    )
);

CREATE INDEX ix_conversation_activities_conversation_time
    ON conversation_activities (conversation_id, created_at ASC);

CREATE TABLE processed_events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    source VARCHAR(80) NOT NULL,
    correlation_id VARCHAR(120),
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX ix_outbox_events_status_available_at ON outbox_events (status, available_at ASC);
