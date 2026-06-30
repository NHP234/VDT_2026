package com.vdt2026.omnicare.channel.delivery.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SimulatedReplyDeliveryService {

    static final String SUCCEEDED_TOPIC = "channel.reply-delivery-succeeded.v1";
    static final String FAILED_TOPIC = "channel.reply-delivery-failed.v1";
    private static final String SOURCE = "channel-service.delivery-simulator";

    private final DeliveryResultPublisher deliveryResultPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SimulatedReplyDeliveryService(DeliveryResultPublisher deliveryResultPublisher, ObjectMapper objectMapper) {
        this(deliveryResultPublisher, objectMapper, Clock.systemUTC());
    }

    SimulatedReplyDeliveryService(DeliveryResultPublisher deliveryResultPublisher, ObjectMapper objectMapper, Clock clock) {
        this.deliveryResultPublisher = deliveryResultPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public DeliveryResult deliver(ReplyRequestEvent event) {
        validate(event);
        boolean failed = event.payload().content().contains("[fail]");
        String topic = failed ? FAILED_TOPIC : SUCCEEDED_TOPIC;
        String eventType = failed ? "reply-delivery-failed" : "reply-delivery-succeeded";
        String eventJson = toEnvelopeJson(event, eventType, failed);
        String key = event.payload().messageId().toString();

        deliveryResultPublisher.publish(topic, key, eventJson);
        return new DeliveryResult(topic, key, eventType);
    }

    private String toEnvelopeJson(ReplyRequestEvent requestEvent, String eventType, boolean failed) {
        Instant now = Instant.now(clock);
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("messageId", requestEvent.payload().messageId().toString());
            payload.put("conversationId", requestEvent.payload().conversationId().toString());
            payload.put("channel", requestEvent.payload().channel());
            payload.put("sourceType", requestEvent.payload().sourceType());
            payload.put("providerAccountId", requestEvent.payload().providerAccountId());
            payload.put("externalConversationId", requestEvent.payload().externalConversationId());
            payload.put("providerMessageId", providerMessageId(requestEvent));
            payload.put("deliveredAt", now.toString());
            if (failed) {
                payload.put("failureReason", "Simulated provider delivery failure");
            }

            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("eventId", UUID.randomUUID().toString());
            envelope.put("eventType", eventType);
            envelope.put("occurredAt", now.toString());
            envelope.put("correlationId", requestEvent.correlationId());
            envelope.put("source", SOURCE);
            envelope.set("payload", payload);
            return objectMapper.writeValueAsString(envelope);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize delivery result event", ex);
        }
    }

    private String providerMessageId(ReplyRequestEvent requestEvent) {
        return "simulated:" + requestEvent.payload().messageId();
    }

    private void validate(ReplyRequestEvent event) {
        if (event == null || event.payload() == null || event.eventId() == null) {
            throw new IllegalArgumentException("Reply request event envelope is required");
        }
        if (!"reply-requested".equals(event.eventType()) && !"reply-retry-requested".equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported reply request event type");
        }
        requireText(event.source(), "Reply request source is required");
        ReplyRequestPayload payload = event.payload();
        if (payload.messageId() == null || payload.conversationId() == null) {
            throw new IllegalArgumentException("Reply request message and conversation IDs are required");
        }
        requireText(payload.channel(), "Reply request channel is required");
        requireText(payload.sourceType(), "Reply request source type is required");
        requireText(payload.providerAccountId(), "Reply request provider account ID is required");
        requireText(payload.externalConversationId(), "Reply request external conversation ID is required");
        requireText(payload.content(), "Reply request content is required");
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    public record DeliveryResult(String topic, String key, String eventType) {
    }
}
