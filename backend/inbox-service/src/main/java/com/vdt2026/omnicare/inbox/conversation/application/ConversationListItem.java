package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.time.Instant;
import java.util.UUID;

public record ConversationListItem(
    UUID id,
    String customerDisplayName,
    Channel channel,
    ConversationSourceType sourceType,
    String channelIdentity,
    String lastMessagePreview,
    Instant lastActivityAt,
    ConversationStatus status,
    AgentSummary assignedAgent
) {
}
