package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import java.util.UUID;

public record AgentSummary(
    UUID id,
    String email,
    String displayName
) {

    static AgentSummary from(AgentEntity agent) {
        if (agent == null) {
            return null;
        }
        return new AgentSummary(agent.id(), agent.email(), agent.displayName());
    }
}
