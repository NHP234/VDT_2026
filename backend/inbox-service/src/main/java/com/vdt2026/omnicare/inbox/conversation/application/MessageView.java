package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import java.time.Instant;
import java.util.UUID;

public record MessageView(
    UUID id,
    MessageDirection direction,
    DeliveryStatus deliveryStatus,
    String externalMessageId,
    String content,
    Instant occurredAt,
    Instant createdAt
) {
}
