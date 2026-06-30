package com.vdt2026.omnicare.inbox.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationRepository;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerEntity;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConversationCommandServiceTests {

    private static final UUID CONVERSATION_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OLD_ASSIGNEE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID NEW_ASSIGNEE_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-06-30T04:00:00Z");

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final ConversationActivityRepository activityRepository = mock(ConversationActivityRepository.class);
    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ConversationCommandService service = new ConversationCommandService(
        conversationRepository,
        activityRepository,
        agentRepository,
        clock
    );

    private final AgentEntity actor = agent(ACTOR_ID, "agent@example.test", "Agent One");
    private final AgentEntity oldAssignee = agent(OLD_ASSIGNEE_ID, "old.agent@example.test", "Old Agent");
    private final AgentEntity newAssignee = agent(NEW_ASSIGNEE_ID, "new.agent@example.test", "New Agent");
    private final AuthenticatedAgent currentAgent = new AuthenticatedAgent(ACTOR_ID, "agent@example.test", "Agent One");

    @BeforeEach
    void setUp() {
        when(activityRepository.save(any(ConversationActivityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRepository.findByIdAndActiveTrue(ACTOR_ID)).thenReturn(Optional.of(actor));
    }

    @Test
    void changeAssigneeUpdatesConversationAndRecordsAuditActivity() {
        ConversationEntity conversation = conversation(oldAssignee);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(agentRepository.findByIdAndActiveTrue(NEW_ASSIGNEE_ID)).thenReturn(Optional.of(newAssignee));

        service.changeAssignee(CONVERSATION_ID, NEW_ASSIGNEE_ID, currentAgent);

        assertThat(conversation.assignedAgent()).isEqualTo(newAssignee);
        ArgumentCaptor<ConversationActivityEntity> activityCaptor = ArgumentCaptor.forClass(ConversationActivityEntity.class);
        verify(activityRepository).save(activityCaptor.capture());
        ConversationActivityEntity activity = activityCaptor.getValue();
        assertThat(activity.activityType()).isEqualTo(ConversationActivityType.ASSIGNMENT_CHANGED);
        assertThat(activity.actorAgent()).isEqualTo(actor);
        assertThat(activity.oldValue()).isEqualTo("old.agent@example.test");
        assertThat(activity.newValue()).isEqualTo("new.agent@example.test");
        assertThat(activity.createdAt()).isEqualTo(NOW);
    }

    @Test
    void changeAssigneeRecordsUnassignAuditActivity() {
        ConversationEntity conversation = conversation(oldAssignee);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));

        service.changeAssignee(CONVERSATION_ID, null, currentAgent);

        assertThat(conversation.assignedAgent()).isNull();
        ArgumentCaptor<ConversationActivityEntity> activityCaptor = ArgumentCaptor.forClass(ConversationActivityEntity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().activityType()).isEqualTo(ConversationActivityType.ASSIGNMENT_CHANGED);
        assertThat(activityCaptor.getValue().oldValue()).isEqualTo("old.agent@example.test");
        assertThat(activityCaptor.getValue().newValue()).isEqualTo("Unassigned");
    }

    @Test
    void changeAssigneeDoesNotRecordAuditWhenTargetAgentIsMissing() {
        ConversationEntity conversation = conversation(oldAssignee);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(agentRepository.findByIdAndActiveTrue(NEW_ASSIGNEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeAssignee(CONVERSATION_ID, NEW_ASSIGNEE_ID, currentAgent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Assigned agent not found");

        assertThat(conversation.assignedAgent()).isEqualTo(oldAssignee);
        verify(activityRepository, never()).save(any(ConversationActivityEntity.class));
    }

    private ConversationEntity conversation(AgentEntity assignee) {
        CustomerEntity customer = new CustomerEntity(
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            "Demo Customer",
            NOW
        );
        ChannelIdentityEntity identity = new ChannelIdentityEntity(
            UUID.fromString("31000000-0000-0000-0000-000000000001"),
            customer,
            Channel.FACEBOOK,
            "local-page-id",
            "fb-user-1",
            "Facebook User",
            NOW
        );
        return new ConversationEntity(
            CONVERSATION_ID,
            customer,
            identity,
            Channel.FACEBOOK,
            ConversationSourceType.MESSAGE,
            ConversationStatus.OPEN,
            assignee,
            "local-page-id",
            "facebook:messenger:local-page-id:fb-user-1",
            null,
            "Hello",
            NOW,
            NOW,
            NOW
        );
    }

    private AgentEntity agent(UUID id, String email, String displayName) {
        return new AgentEntity(id, email, displayName, "{noop}password", true, NOW);
    }
}
