package com.vdt2026.omnicare.inbox.conversation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageRepository;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplyCommandService {

    public static final int MAX_REPLY_LENGTH = 10_000;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationActivityRepository activityRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ReplyCommandService(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository,
        OutboxEventRepository outboxEventRepository,
        AgentRepository agentRepository,
        ObjectMapper objectMapper
    ) {
        this(conversationRepository, messageRepository, activityRepository, outboxEventRepository, agentRepository, objectMapper, Clock.systemUTC());
    }

    ReplyCommandService(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository,
        OutboxEventRepository outboxEventRepository,
        AgentRepository agentRepository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public UUID queueReply(UUID conversationId, String content, AuthenticatedAgent currentAgent) {
        String normalizedContent = normalizeReplyContent(content);
        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        AgentEntity actor = actingAgent(currentAgent);
        Instant now = Instant.now(clock);

        MessageEntity message = messageRepository.save(new MessageEntity(
            null,
            conversation,
            conversation.channel(),
            MessageDirection.OUTBOUND,
            DeliveryStatus.QUEUED,
            conversation.providerAccountId(),
            null,
            normalizedContent,
            now,
            now
        ));

        conversation.updateLastMessage(preview(normalizedContent), now);
        activityRepository.save(new ConversationActivityEntity(
            null,
            conversation,
            actor,
            ConversationActivityType.REPLY_QUEUED,
            null,
            message.id().toString(),
            now
        ));
        createOutboxEvent(conversation, message, "inbox.reply-requested.v1", now);
        return message.id();
    }

    @Transactional
    public UUID retryFailedMessage(UUID messageId, AuthenticatedAgent currentAgent) {
        MessageEntity message = messageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        if (message.direction() != MessageDirection.OUTBOUND) {
            throw new IllegalArgumentException("Only outbound messages can be retried");
        }
        if (message.deliveryStatus() != DeliveryStatus.FAILED) {
            throw new IllegalArgumentException("Only failed messages can be retried");
        }

        AgentEntity actor = actingAgent(currentAgent);
        Instant now = Instant.now(clock);
        message.markDeliveryStatus(DeliveryStatus.QUEUED);
        activityRepository.save(new ConversationActivityEntity(
            null,
            message.conversation(),
            actor,
            ConversationActivityType.DELIVERY_STATUS_CHANGED,
            DeliveryStatus.FAILED.name(),
            DeliveryStatus.QUEUED.name(),
            now
        ));
        createOutboxEvent(message.conversation(), message, "inbox.reply-retry-requested.v1", now);
        return message.id();
    }

    private String normalizeReplyContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Reply content is required");
        }
        String trimmed = content.trim();
        if (trimmed.length() > MAX_REPLY_LENGTH) {
            throw new IllegalArgumentException("Reply content must be 10000 characters or fewer");
        }
        return trimmed;
    }

    private AgentEntity actingAgent(AuthenticatedAgent currentAgent) {
        return agentRepository.findByIdAndActiveTrue(currentAgent.id())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated agent not found"));
    }

    private String preview(String content) {
        if (content.length() <= 500) {
            return content;
        }
        return content.substring(0, 500);
    }

    private void createOutboxEvent(ConversationEntity conversation, MessageEntity message, String eventType, Instant now) {
        outboxEventRepository.save(new OutboxEventEntity(
            null,
            "Message",
            message.id(),
            eventType,
            payload(conversation, message),
            OutboxEventEntity.OutboxStatus.PENDING,
            0,
            now,
            now,
            null
        ));
    }

    private String payload(ConversationEntity conversation, MessageEntity message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "messageId", message.id(),
                "conversationId", conversation.id(),
                "channel", conversation.channel(),
                "sourceType", conversation.sourceType(),
                "providerAccountId", conversation.providerAccountId(),
                "externalConversationId", conversation.externalConversationId(),
                "content", message.content()
            ));
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize reply outbox payload", ex);
        }
    }
}
