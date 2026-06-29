package com.vdt2026.omnicare.inbox.identity.interfaces;

import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import java.util.UUID;

public record AgentResponse(
    UUID id,
    String email,
    String displayName
) {

    static AgentResponse from(AuthenticatedAgent agent) {
        return new AgentResponse(agent.id(), agent.email(), agent.displayName());
    }

    static AgentResponse from(AgentEntity agent) {
        return new AgentResponse(agent.id(), agent.email(), agent.displayName());
    }
}
