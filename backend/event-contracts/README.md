# Event Contracts

Kafka is used only for asynchronous facts required by `FR-09`.

## Envelope

All events should include:

```json
{
  "eventId": "uuid",
  "eventType": "past-tense-event-name",
  "occurredAt": "2026-06-18T00:00:00Z",
  "correlationId": "uuid-or-trace-id",
  "source": "service-or-adapter-name",
  "payload": {}
}
```

Rules:

- Use UTC ISO 8601 timestamps.
- Do not place secrets or unnecessary personal data in events.
- Keep payload schemas backward compatible within a topic version.
- Consumers must be idempotent because delivery is at least once.

## Initial Topics

| Topic | Producer | Consumer | Purpose | Requirement |
| --- | --- | --- | --- | --- |
| `inbox.message-received.v1` | Channel service | Inbox service | Normalized inbound Facebook/email message was received. | `FR-09` |
| `inbox.reply-requested.v1` | Inbox service | Channel service | Agent requested an outbound reply delivery. | `FR-06`, `FR-09` |
| `inbox.reply-retry-requested.v1` | Inbox service | Channel service | Agent retried a failed outbound reply delivery. | `FR-06`, `FR-09` |
| `channel.reply-delivery-succeeded.v1` | Channel service | Inbox service | Provider accepted or completed outbound delivery. | `FR-06`, `FR-09` |
| `channel.reply-delivery-failed.v1` | Channel service | Inbox service | Provider delivery failed and should be visible to the agent. | `FR-06`, `FR-09` |

Kafka consumers use bounded retry before handing a failed record to a
dead-letter topic. Local defaults are 3 total delivery attempts with a 1000 ms
fixed backoff. Dead-letter topic names append `.dlq` to the source topic name
and keep the original partition, for example
`inbox.message-received.v1.dlq`.

## `inbox.message-received.v1`

The first checked-in producer path is the Facebook simulator in Channel service.
Channel service publishes the normalized event to Kafka using the external
conversation ID as the record key, so messages for the same conversation remain
ordered within a partition. Timestamp fields are serialized as ISO 8601 UTC
strings, not numeric epoch timestamps.

Inbox service consumes this topic and persists the payload idempotently. It
records processed envelope IDs in `processed_events` and also checks the
provider `externalMessageId` before inserting a new inbound message.
Channel service also performs short-lived Redis deduplication before publishing
simulator/webhook inbound events. The Redis key is derived from channel,
provider account, source type, and provider message/comment ID. If Redis is
unavailable, Channel publishes anyway and relies on Inbox PostgreSQL idempotency
to avoid losing customer events.

Payload fields:

```json
{
  "channel": "FACEBOOK",
  "sourceType": "MESSAGE",
  "providerAccountId": "local-page-id",
  "externalConversationId": "facebook:messenger:local-page-id:fb-user-c",
  "externalMessageId": "mid.local.facebook.messenger.1001",
  "externalIdentityId": "fb-user-c",
  "customerDisplayName": "Le Van C",
  "subject": null,
  "content": "Plain text content",
  "occurredAt": "2026-06-29T02:15:00Z"
}
```

Facebook conversation key rules:

- Messenger messages group by Facebook Page and sender identity:
  `facebook:messenger:{pageId}:{senderId}`.
- Page comments group by Facebook Page, post, and root comment:
  `facebook:comment:{pageId}:{postId}:{rootCommentId}`.
- Messenger and comment conversations never merge.

Email conversation key rules:

- Email messages use channel `EMAIL` and source type `EMAIL`.
- Simulator and IMAP email events group by provider mailbox and thread root message ID:
  `email:{providerAccountId}:{rootMessageId}`.
- The thread root is resolved from the first `References` header when present,
  then `In-Reply-To`, then the message's own `Message-ID`.
- Subject is optional and stored only as normalized conversation metadata; it is
  not used to merge threads.
- IMAP polling reads unseen messages from the configured folder, maps only safe
  plain-text content, ignores attachments, publishes through the same
  deduplicated Kafka path, and marks processed or invalid messages as seen.

## `inbox.reply-requested.v1`

Inbox service writes outbound reply requests to `outbox_events` in the same
database transaction that creates the queued outbound message. A scheduled
publisher sends pending outbox rows to Kafka and marks them `PUBLISHED` only
after Kafka acknowledges the send.

The Kafka record key is the internal outbound message ID. The envelope uses the
outbox row ID as `eventId` and `correlationId`.

Payload fields:

```json
{
  "messageId": "50000000-0000-0000-0000-000000000001",
  "conversationId": "40000000-0000-0000-0000-000000000001",
  "channel": "FACEBOOK",
  "sourceType": "MESSAGE",
  "providerAccountId": "local-page-id",
  "externalConversationId": "messenger:fb-user-a",
  "externalIdentityId": "fb-user-a",
  "subject": null,
  "content": "Plain text reply"
}
```

`inbox.reply-retry-requested.v1` currently uses the same payload shape.
For email replies, `externalIdentityId` is the recipient email address and
`subject` is used only for the SMTP subject line. Email SMTP delivery derives
`In-Reply-To` and `References` from the normalized email conversation ID:
`email:{providerAccountId}:{rootMessageId}`.

## `channel.reply-delivery-succeeded.v1`

Channel service consumes `inbox.reply-requested.v1` and
`inbox.reply-retry-requested.v1`, sends the reply through the configured
outbound sender, then publishes a delivery result. Local simulator mode uses a
deterministic sender: normal content succeeds, and content containing `[fail]`
publishes the failed topic so the demo can exercise retry and failure
visibility without real provider credentials. Real Facebook mode uses the Graph
API adapter contract: Messenger replies call `/{pageId}/messages`, while public
comment replies call `/{commentId}/comments`. Email replies are sent through
SMTP using the configured `spring.mail` host and preserve `In-Reply-To` and
`References` headers from the normalized thread root.

The Kafka record key is the internal outbound message ID.
Inbox service consumes the result event idempotently, updates the outbound
message delivery status to `SENT`, stores `providerMessageId`, and records a
`DELIVERY_STATUS_CHANGED` activity when the status changes.

Payload fields:

```json
{
  "messageId": "50000000-0000-0000-0000-000000000001",
  "conversationId": "40000000-0000-0000-0000-000000000001",
  "channel": "FACEBOOK",
  "sourceType": "MESSAGE",
  "providerAccountId": "local-page-id",
  "externalConversationId": "messenger:fb-user-a",
  "providerMessageId": "simulated:50000000-0000-0000-0000-000000000001",
  "deliveredAt": "2026-06-30T03:30:00Z"
}
```

## `channel.reply-delivery-failed.v1`

Uses the same payload as the success event and adds `failureReason`.
Inbox service updates the outbound message delivery status to `FAILED` and
records the failure reason in the audit activity value.

```json
{
  "messageId": "50000000-0000-0000-0000-000000000001",
  "conversationId": "40000000-0000-0000-0000-000000000001",
  "channel": "FACEBOOK",
  "sourceType": "MESSAGE",
  "providerAccountId": "local-page-id",
  "externalConversationId": "messenger:fb-user-a",
  "providerMessageId": "simulated:50000000-0000-0000-0000-000000000001",
  "deliveredAt": "2026-06-30T03:30:00Z",
  "failureReason": "Simulated provider delivery failure"
}
```
