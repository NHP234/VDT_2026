package com.vdt2026.omnicare.channel.delivery.application;

import java.util.UUID;

public record ReplyRequestPayload(
    UUID messageId,
    UUID conversationId,
    String channel,
    String sourceType,
    String providerAccountId,
    String externalConversationId,
    String content
) {
}
