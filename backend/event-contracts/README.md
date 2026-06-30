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

Dead-letter topic names should append `.dlq` to the source topic name unless an
ADR chooses a different convention.

## `inbox.message-received.v1`

The first checked-in producer path is the Facebook simulator in Channel service.
Channel service publishes the normalized event to Kafka using the external
conversation ID as the record key, so messages for the same conversation remain
ordered within a partition. Timestamp fields are serialized as ISO 8601 UTC
strings, not numeric epoch timestamps.

Inbox service consumes this topic and persists the payload idempotently. It
records processed envelope IDs in `processed_events` and also checks the
provider `externalMessageId` before inserting a new inbound message.

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
  "content": "Plain text reply"
}
```

`inbox.reply-retry-requested.v1` currently uses the same payload shape.

## `channel.reply-delivery-succeeded.v1`

Channel service currently uses a deterministic simulator delivery adapter for
the MVP. It consumes `inbox.reply-requested.v1` and
`inbox.reply-retry-requested.v1`, then publishes a delivery result. Normal
content succeeds; content containing `[fail]` publishes the failed topic so the
demo can exercise retry and failure visibility without real provider
credentials.

The Kafka record key is the internal outbound message ID.

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
