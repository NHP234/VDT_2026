package com.vdt2026.omnicare.channel.delivery.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReplyDeliveryService {

    static final String SUCCEEDED_TOPIC = "channel.reply-delivery-succeeded.v1";
    static final String FAILED_TOPIC = "channel.reply-delivery-failed.v1";
    private static final String SOURCE = "channel-service.reply-delivery";
    private static final String OUTBOUND_DELIVERIES_METRIC = "omnicare.outbound.deliveries";

    private final DeliveryResultPublisher deliveryResultPublisher;
    private final List<OutboundReplySender> outboundReplySenders;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ReplyDeliveryService(
        DeliveryResultPublisher deliveryResultPublisher,
        List<OutboundReplySender> outboundReplySenders,
        ObjectMapper objectMapper,
        ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        this(
            deliveryResultPublisher,
            outboundReplySenders,
            objectMapper,
            Clock.systemUTC(),
            meterRegistryProvider.getIfAvailable(() -> Metrics.globalRegistry)
        );
    }

    ReplyDeliveryService(
        DeliveryResultPublisher deliveryResultPublisher,
        List<OutboundReplySender> outboundReplySenders,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this(deliveryResultPublisher, outboundReplySenders, objectMapper, clock, Metrics.globalRegistry);
    }

    ReplyDeliveryService(
        DeliveryResultPublisher deliveryResultPublisher,
        List<OutboundReplySender> outboundReplySenders,
        ObjectMapper objectMapper,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.deliveryResultPublisher = deliveryResultPublisher;
        this.outboundReplySenders = outboundReplySenders;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    public DeliveryResult deliver(ReplyRequestEvent event) {
        validate(event);
        OutboundReplyResult providerResult = null;
        OutboundReplyDeliveryException failure = null;
        try {
            providerResult = outboundReplySender(event.payload()).send(event.payload());
        }
        catch (OutboundReplyDeliveryException ex) {
            failure = ex;
        }

        boolean failed = failure != null;
        String topic = failed ? FAILED_TOPIC : SUCCEEDED_TOPIC;
        String eventType = failed ? "reply-delivery-failed" : "reply-delivery-succeeded";
        String eventJson = toEnvelopeJson(event, eventType, providerMessageId(event, providerResult, failure), failure);
        String key = event.payload().messageId().toString();

        deliveryResultPublisher.publish(topic, key, eventJson);
        incrementOutboundMetric(failed ? "failed" : "sent", event.payload());
        return new DeliveryResult(topic, key, eventType);
    }

    private String toEnvelopeJson(
        ReplyRequestEvent requestEvent,
        String eventType,
        String providerMessageId,
        OutboundReplyDeliveryException failure
    ) {
        Instant now = Instant.now(clock);
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("messageId", requestEvent.payload().messageId().toString());
            payload.put("conversationId", requestEvent.payload().conversationId().toString());
            payload.put("channel", requestEvent.payload().channel());
            payload.put("sourceType", requestEvent.payload().sourceType());
            payload.put("providerAccountId", requestEvent.payload().providerAccountId());
            payload.put("externalConversationId", requestEvent.payload().externalConversationId());
            payload.put("providerMessageId", providerMessageId);
            payload.put("deliveredAt", now.toString());
            if (failure != null) {
                payload.put("failureReason", failure.getMessage());
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

    private String providerMessageId(
        ReplyRequestEvent requestEvent,
        OutboundReplyResult providerResult,
        OutboundReplyDeliveryException failure
    ) {
        if (providerResult != null && StringUtils.hasText(providerResult.providerMessageId())) {
            return providerResult.providerMessageId();
        }
        if (failure != null && StringUtils.hasText(failure.providerMessageId())) {
            return failure.providerMessageId();
        }
        return "failed:" + requestEvent.payload().messageId();
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

    private OutboundReplySender outboundReplySender(ReplyRequestPayload payload) {
        return outboundReplySenders.stream()
            .filter(sender -> sender.supports(payload))
            .findFirst()
            .orElseThrow(() -> new OutboundReplyDeliveryException(
                "No outbound reply sender for channel %s and source type %s".formatted(payload.channel(), payload.sourceType())
            ));
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void incrementOutboundMetric(String result, ReplyRequestPayload payload) {
        Counter.builder(OUTBOUND_DELIVERIES_METRIC)
            .description("Outbound reply deliveries published by channel-service")
            .tag("result", result)
            .tag("channel", tagValue(payload.channel()))
            .tag("sourceType", tagValue(payload.sourceType()))
            .register(meterRegistry)
            .increment();
    }

    private String tagValue(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    public record DeliveryResult(String topic, String key, String eventType) {
    }
}
