package com.vdt2026.omnicare.inbox.conversation.application;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResultPayload(
    UUID messageId,
    UUID conversationId,
    String channel,
    String sourceType,
    String providerAccountId,
    String externalConversationId,
    String providerMessageId,
    Instant deliveredAt,
    String failureReason
) {
}
