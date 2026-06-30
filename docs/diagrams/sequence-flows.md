# Sequence Flows

These sequences document the implemented demo flows and the production-facing
adapters they prepare for. Real Meta and live mailbox credentials are still
human-owned setup items.

## Facebook Inbound Message Or Comment

```mermaid
sequenceDiagram
    autonumber
    participant Meta as Meta / Simulator
    participant Channel as Channel service
    participant Redis as Redis
    participant Kafka as Kafka
    participant Inbox as Inbox service
    participant DB as PostgreSQL
    participant UI as React frontend

    Meta->>Channel: POST /webhooks/facebook or simulator fixture
    Channel->>Channel: verify token/signature when real mode is enabled
    Channel->>Channel: normalize Messenger message or Page comment
    Channel->>Redis: SET dedup key with TTL
    alt duplicate key exists
        Channel-->>Meta: 200 accepted, skip publish
    else first delivery
        Channel->>Kafka: publish inbox.message-received.v1
        Kafka->>Inbox: consume inbound event
        Inbox->>DB: upsert customer/channel identity/conversation/message
        Inbox->>DB: record processed event for durable idempotency
        UI->>Inbox: GET /api/v1/conversations
        Inbox-->>UI: unified conversation list/detail
    end
```

## Email Inbound Message

```mermaid
sequenceDiagram
    autonumber
    participant Mail as Mailbox / Email simulator
    participant Channel as Channel service
    participant Redis as Redis
    participant Kafka as Kafka
    participant Inbox as Inbox service
    participant DB as PostgreSQL
    participant UI as React frontend

    Mail->>Channel: IMAP poll result or simulator fixture
    Channel->>Channel: parse sender, subject, body, Message-ID, References
    Channel->>Redis: SET dedup key with TTL
    alt duplicate Message-ID or event key
        Channel-->>Mail: skip duplicate publication
    else first delivery
        Channel->>Kafka: publish inbox.message-received.v1
        Kafka->>Inbox: consume inbound event
        Inbox->>DB: create or reopen email conversation by thread root
        Inbox->>DB: store inbound message and processed event
        UI->>Inbox: GET /api/v1/conversations?channel=EMAIL
        Inbox-->>UI: email conversation appears in inbox
    end
```

## Outbound Reply And Delivery Result

```mermaid
sequenceDiagram
    autonumber
    participant Agent as Agent
    participant UI as React frontend
    participant Inbox as Inbox service
    participant DB as PostgreSQL
    participant Kafka as Kafka
    participant Channel as Channel service
    participant Provider as Meta / SMTP / Simulator

    Agent->>UI: write plain-text reply
    UI->>Inbox: POST /api/v1/conversations/{id}/messages
    Inbox->>DB: store outbound message as QUEUED
    Inbox->>DB: insert outbox reply-request event
    Inbox-->>UI: return message with delivery status
    Inbox->>Kafka: scheduled outbox publisher sends reply-request.v1
    Kafka->>Channel: consume reply request
    Channel->>Provider: send via original channel
    alt provider accepts
        Channel->>Kafka: publish delivery-result.v1 SENT
    else provider rejects or timeout exhausted
        Channel->>Kafka: publish delivery-result.v1 FAILED
    end
    Kafka->>Inbox: consume delivery result
    Inbox->>DB: update outbound message status idempotently
    UI->>Inbox: refresh conversation detail
    Inbox-->>UI: show SENT or FAILED with retry option
```

