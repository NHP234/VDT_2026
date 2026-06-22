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
| `channel.reply-delivery-succeeded.v1` | Channel service | Inbox service | Provider accepted or completed outbound delivery. | `FR-06`, `FR-09` |
| `channel.reply-delivery-failed.v1` | Channel service | Inbox service | Provider delivery failed and should be visible to the agent. | `FR-06`, `FR-09` |

Dead-letter topic names should append `.dlq` to the source topic name unless an
ADR chooses a different convention.
