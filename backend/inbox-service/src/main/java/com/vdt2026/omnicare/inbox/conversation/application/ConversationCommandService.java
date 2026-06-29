package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.AssignmentPolicy;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationRepository;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationCommandService {

    private final ConversationRepository conversationRepository;
    private final ConversationActivityRepository activityRepository;
    private final AgentRepository agentRepository;
    private final Clock clock;

    @Autowired
    public ConversationCommandService(
        ConversationRepository conversationRepository,
        ConversationActivityRepository activityRepository,
        AgentRepository agentRepository
    ) {
        this(conversationRepository, activityRepository, agentRepository, Clock.systemUTC());
    }

    ConversationCommandService(
        ConversationRepository conversationRepository,
        ConversationActivityRepository activityRepository,
        AgentRepository agentRepository,
        Clock clock
    ) {
        this.conversationRepository = conversationRepository;
        this.activityRepository = activityRepository;
        this.agentRepository = agentRepository;
        this.clock = clock;
    }

    @Transactional
    public void changeStatus(UUID conversationId, ConversationStatus newStatus, AuthenticatedAgent currentAgent) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        ConversationStatus oldStatus = conversation.status();
        conversation.changeStatus(newStatus);

        activityRepository.save(new ConversationActivityEntity(
            null,
            conversation,
            actingAgent(currentAgent),
            ConversationActivityType.STATUS_CHANGED,
            oldStatus.name(),
            newStatus.name(),
            Instant.now(clock)
        ));
    }

    @Transactional
    public void changeAssignee(UUID conversationId, UUID assignedAgentId, AuthenticatedAgent currentAgent) {
        if (!AssignmentPolicy.canAssign(currentAgent.id(), assignedAgentId)) {
            throw new IllegalArgumentException("Authenticated agent is required for assignment");
        }

        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        AgentEntity oldAssignee = conversation.assignedAgent();
        AgentEntity newAssignee = assignedAgentId == null ? null : agentRepository.findByIdAndActiveTrue(assignedAgentId)
            .orElseThrow(() -> new IllegalArgumentException("Assigned agent not found"));

        if (newAssignee == null) {
            conversation.unassign();
        }
        else {
            conversation.assignTo(newAssignee);
        }

        activityRepository.save(new ConversationActivityEntity(
            null,
            conversation,
            actingAgent(currentAgent),
            ConversationActivityType.ASSIGNMENT_CHANGED,
            assigneeValue(oldAssignee),
            assigneeValue(newAssignee),
            Instant.now(clock)
        ));
    }

    private AgentEntity actingAgent(AuthenticatedAgent currentAgent) {
        return agentRepository.findByIdAndActiveTrue(currentAgent.id())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated agent not found"));
    }

    private String assigneeValue(AgentEntity agent) {
        if (agent == null) {
            return "Unassigned";
        }
        return agent.email();
    }
}
