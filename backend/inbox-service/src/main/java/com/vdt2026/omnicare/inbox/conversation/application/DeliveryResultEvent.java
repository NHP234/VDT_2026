package com.vdt2026.omnicare.inbox.conversation.application;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResultEvent(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String correlationId,
    String source,
    DeliveryResultPayload payload
) {
}
