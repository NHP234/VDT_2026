package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.time.Instant;

public record InboundMessageReceivedPayload(
    Channel channel,
    ConversationSourceType sourceType,
    String providerAccountId,
    String externalConversationId,
    String externalMessageId,
    String externalIdentityId,
    String customerDisplayName,
    String content,
    Instant occurredAt
) {
}
