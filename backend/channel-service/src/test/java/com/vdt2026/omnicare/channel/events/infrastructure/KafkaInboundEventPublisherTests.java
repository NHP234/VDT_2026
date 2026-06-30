package com.vdt2026.omnicare.channel.events.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaInboundEventPublisherTests {

    @Test
    void publishesInboundMessageEventWithConversationKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, EventEnvelope<NormalizedInboundMessagePayload>> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaInboundEventPublisher publisher = new KafkaInboundEventPublisher(kafkaTemplate);
        EventEnvelope<NormalizedInboundMessagePayload> event = event();

        publisher.publish(event);

        verify(kafkaTemplate).send(
            "inbox.message-received.v1",
            "facebook:messenger:local-page-id:fb-user-c",
            event
        );
        verify(kafkaTemplate).flush();
    }

    private EventEnvelope<NormalizedInboundMessagePayload> event() {
        Instant occurredAt = Instant.parse("2026-06-29T02:15:00Z");
        NormalizedInboundMessagePayload payload = new NormalizedInboundMessagePayload(
            "FACEBOOK",
            "MESSAGE",
            "local-page-id",
            "facebook:messenger:local-page-id:fb-user-c",
            "mid.local.facebook.messenger.1001",
            "fb-user-c",
            "Le Van C",
            "Shop oi san pham nay con hang khong?",
            occurredAt
        );

        return new EventEnvelope<>(
            UUID.fromString("70000000-0000-0000-0000-000000000001"),
            "message-received",
            occurredAt,
            "corr-test-1",
            "channel-service.facebook-simulator",
            payload
        );
    }
}
