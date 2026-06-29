package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import java.time.Instant;
import java.util.UUID;

public record ActivityView(
    UUID id,
    ConversationActivityType activityType,
    AgentSummary actorAgent,
    String oldValue,
    String newValue,
    Instant createdAt
) {
}
