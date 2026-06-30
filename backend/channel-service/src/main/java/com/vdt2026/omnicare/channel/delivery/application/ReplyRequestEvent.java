package com.vdt2026.omnicare.channel.delivery.application;

import java.time.Instant;
import java.util.UUID;

public record ReplyRequestEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String correlationId,
    String source,
    ReplyRequestPayload payload
) {
}
