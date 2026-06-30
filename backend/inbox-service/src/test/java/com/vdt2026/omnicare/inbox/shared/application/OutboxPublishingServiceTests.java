package com.vdt2026.omnicare.inbox.shared.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity.OutboxStatus;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

class OutboxPublishingServiceTests {

    private final OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
    private final OutboxEventPublisher outboxEventPublisher = mock(OutboxEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T03:00:00Z"), ZoneOffset.UTC);
    private final OutboxPublishingService service = new OutboxPublishingService(
        outboxEventRepository,
        outboxEventPublisher,
        objectMapper,
        clock
    );

    @Test
    void publishesPendingOutboxEventAsKafkaEnvelopeAndMarksItPublished() throws Exception {
        OutboxEventEntity outboxEvent = new OutboxEventEntity(
            UUID.fromString("80000000-0000-0000-0000-000000000001"),
            "Message",
            UUID.fromString("50000000-0000-0000-0000-000000000001"),
            "inbox.reply-requested.v1",
            """
                {
                  "messageId": "50000000-0000-0000-0000-000000000001",
                  "conversationId": "40000000-0000-0000-0000-000000000001",
                  "channel": "FACEBOOK",
                  "content": "Hello"
                }
                """,
            OutboxStatus.PENDING,
            0,
            Instant.parse("2026-06-30T02:59:00Z"),
            Instant.parse("2026-06-30T02:59:00Z"),
            null
        );
        when(outboxEventRepository.findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
            eq(OutboxStatus.PENDING),
            eq(Instant.parse("2026-06-30T03:00:00Z")),
            any(Pageable.class)
        )).thenReturn(List.of(outboxEvent));

        int published = service.publishDueEvents();

        assertThat(published).isEqualTo(1);
        assertThat(outboxEvent.status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-06-30T03:00:00Z"));

        ArgumentCaptor<String> envelopeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventPublisher).publish(
            eq("inbox.reply-requested.v1"),
            eq("50000000-0000-0000-0000-000000000001"),
            envelopeCaptor.capture()
        );
        JsonNode envelope = objectMapper.readTree(envelopeCaptor.getValue());
        assertThat(envelope.path("eventId").asText()).isEqualTo("80000000-0000-0000-0000-000000000001");
        assertThat(envelope.path("eventType").asText()).isEqualTo("reply-requested");
        assertThat(envelope.path("occurredAt").asText()).isEqualTo("2026-06-30T03:00:00Z");
        assertThat(envelope.path("source").asText()).isEqualTo("inbox-service");
        assertThat(envelope.path("payload").path("content").asText()).isEqualTo("Hello");
    }
}
