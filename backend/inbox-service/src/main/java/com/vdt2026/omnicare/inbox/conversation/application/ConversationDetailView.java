package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDetailView(
    UUID id,
    CustomerSummary customer,
    ChannelIdentitySummary channelIdentity,
    Channel channel,
    ConversationSourceType sourceType,
    ConversationStatus status,
    AgentSummary assignedAgent,
    String subject,
    String lastMessagePreview,
    Instant lastActivityAt,
    Instant createdAt,
    Instant updatedAt,
    List<MessageView> messages,
    List<ActivityView> activities
) {
}
