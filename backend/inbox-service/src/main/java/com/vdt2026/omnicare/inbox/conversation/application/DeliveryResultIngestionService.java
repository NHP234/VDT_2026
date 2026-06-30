package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageRepository;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeliveryResultIngestionService {

    private final ProcessedEventRepository processedEventRepository;
    private final MessageRepository messageRepository;
    private final ConversationActivityRepository activityRepository;
    private final Clock clock;

    @Autowired
    public DeliveryResultIngestionService(
        ProcessedEventRepository processedEventRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository
    ) {
        this(processedEventRepository, messageRepository, activityRepository, Clock.systemUTC());
    }

    DeliveryResultIngestionService(
        ProcessedEventRepository processedEventRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository,
        Clock clock
    ) {
        this.processedEventRepository = processedEventRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    @Transactional
    public DeliveryResult ingest(DeliveryResultEvent event) {
        validate(event);
        String eventId = event.eventId().toString();
        if (processedEventRepository.findByEventId(eventId).isPresent()) {
            return DeliveryResult.DUPLICATE_EVENT;
        }

        MessageEntity message = messageRepository.findById(event.payload().messageId())
            .orElseThrow(() -> new IllegalArgumentException("Outbound message not found"));
        if (message.direction() != MessageDirection.OUTBOUND) {
            throw new IllegalArgumentException("Delivery result only applies to outbound messages");
        }

        DeliveryStatus newStatus = statusFrom(event.eventType());
        DeliveryStatus oldStatus = message.deliveryStatus();
        if (oldStatus != newStatus) {
            message.markDeliveryResult(newStatus, event.payload().providerMessageId());
            activityRepository.save(new ConversationActivityEntity(
                UUID.randomUUID(),
                message.conversation(),
                null,
                ConversationActivityType.DELIVERY_STATUS_CHANGED,
                oldStatus.name(),
                activityValue(newStatus, event.payload().failureReason()),
                event.occurredAt()
            ));
        }
        markProcessed(event);
        return oldStatus == newStatus ? DeliveryResult.NO_STATUS_CHANGE : DeliveryResult.UPDATED;
    }

    private DeliveryStatus statusFrom(String eventType) {
        return switch (eventType) {
            case "reply-delivery-succeeded" -> DeliveryStatus.SENT;
            case "reply-delivery-failed" -> DeliveryStatus.FAILED;
            default -> throw new IllegalArgumentException("Unsupported delivery result event type");
        };
    }

    private String activityValue(DeliveryStatus status, String failureReason) {
        if (status != DeliveryStatus.FAILED || !StringUtils.hasText(failureReason)) {
            return status.name();
        }
        return status.name() + ": " + failureReason.trim();
    }

    private void markProcessed(DeliveryResultEvent event) {
        processedEventRepository.save(new ProcessedEventEntity(
            UUID.randomUUID(),
            event.eventId().toString(),
            event.eventType(),
            event.source(),
            event.correlationId(),
            Instant.now(clock)
        ));
    }

    private void validate(DeliveryResultEvent event) {
        if (event == null || event.eventId() == null || event.payload() == null) {
            throw new IllegalArgumentException("Delivery result event envelope is required");
        }
        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("Delivery result event timestamp is required");
        }
        requireText(event.source(), "Delivery result source is required");
        statusFrom(event.eventType());

        DeliveryResultPayload payload = event.payload();
        if (payload.messageId() == null || payload.conversationId() == null) {
            throw new IllegalArgumentException("Delivery result message and conversation IDs are required");
        }
        requireText(payload.channel(), "Delivery result channel is required");
        requireText(payload.sourceType(), "Delivery result source type is required");
        requireText(payload.providerAccountId(), "Delivery result provider account ID is required");
        requireText(payload.externalConversationId(), "Delivery result external conversation ID is required");
        requireText(payload.providerMessageId(), "Delivery result provider message ID is required");
        if (payload.deliveredAt() == null) {
            throw new IllegalArgumentException("Delivery result delivered timestamp is required");
        }
        if ("reply-delivery-failed".equals(event.eventType())) {
            requireText(payload.failureReason(), "Delivery failure reason is required");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    public enum DeliveryResult {
        UPDATED,
        DUPLICATE_EVENT,
        NO_STATUS_CHANGE
    }
}
