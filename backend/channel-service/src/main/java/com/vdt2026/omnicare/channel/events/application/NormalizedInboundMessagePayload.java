package com.vdt2026.omnicare.channel.events.application;

import java.time.Instant;

public record NormalizedInboundMessagePayload(
    String channel,
    String sourceType,
    String providerAccountId,
    String externalConversationId,
    String externalMessageId,
    String externalIdentityId,
    String customerDisplayName,
    String content,
    Instant occurredAt
) {
}
