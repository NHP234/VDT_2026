package com.vdt2026.omnicare.inbox.shared.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity.OutboxStatus;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxPublishingService {

    private static final int DEFAULT_BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public OutboxPublishingService(
        OutboxEventRepository outboxEventRepository,
        OutboxEventPublisher outboxEventPublisher,
        ObjectMapper objectMapper
    ) {
        this(outboxEventRepository, outboxEventPublisher, objectMapper, Clock.systemUTC());
    }

    OutboxPublishingService(
        OutboxEventRepository outboxEventRepository,
        OutboxEventPublisher outboxEventPublisher,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventPublisher = outboxEventPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public int publishDueEvents() {
        List<OutboxEventEntity> events = outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
            OutboxStatus.PENDING,
            Instant.now(clock),
            PageRequest.of(0, DEFAULT_BATCH_SIZE)
        );
        events.forEach(this::publishEvent);
        return events.size();
    }

    private void publishEvent(OutboxEventEntity outboxEvent) {
        outboxEventPublisher.publish(
            outboxEvent.eventType(),
            outboxEvent.aggregateId().toString(),
            toEnvelopeJson(outboxEvent)
        );
        outboxEvent.markPublished(Instant.now(clock));
    }

    private String toEnvelopeJson(OutboxEventEntity outboxEvent) {
        try {
            JsonNode payload = objectMapper.readTree(outboxEvent.payload());
            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("eventId", outboxEvent.id().toString());
            envelope.put("eventType", eventName(outboxEvent.eventType()));
            envelope.put("occurredAt", Instant.now(clock).toString());
            envelope.put("correlationId", outboxEvent.id().toString());
            envelope.put("source", "inbox-service");
            envelope.set("payload", payload);
            return objectMapper.writeValueAsString(envelope);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox event " + outboxEvent.id(), ex);
        }
    }

    private String eventName(String topic) {
        int firstDot = topic.indexOf('.');
        int lastDot = topic.lastIndexOf('.');
        if (firstDot < 0 || lastDot <= firstDot) {
            return topic;
        }
        return topic.substring(firstDot + 1, lastDot);
    }
}
