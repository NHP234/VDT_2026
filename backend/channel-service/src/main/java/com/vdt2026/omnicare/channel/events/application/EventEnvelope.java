package com.vdt2026.omnicare.channel.events.application;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String correlationId,
    String source,
    T payload
) {
}
