package com.vdt2026.omnicare.channel.delivery.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReplyDeliveryServiceTests {

    private final RecordingDeliveryResultPublisher publisher = new RecordingDeliveryResultPublisher();
    private final SimulatedOutboundReplySender outboundReplySender = new SimulatedOutboundReplySender();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ReplyDeliveryService service = new ReplyDeliveryService(
        publisher,
        List.of(outboundReplySender),
        objectMapper,
        Clock.fixed(Instant.parse("2026-06-30T03:30:00Z"), ZoneOffset.UTC)
    );

    @Test
    void publishesSucceededDeliveryResultForNormalReply() throws Exception {
        ReplyRequestEvent event = event("Hello customer");

        ReplyDeliveryService.DeliveryResult result = service.deliver(event);

        assertThat(result.topic()).isEqualTo("channel.reply-delivery-succeeded.v1");
        assertThat(result.key()).isEqualTo("50000000-0000-0000-0000-000000000001");
        assertThat(publisher.events()).hasSize(1);
        PublishedEvent published = publisher.events().getFirst();
        assertThat(published.topic()).isEqualTo("channel.reply-delivery-succeeded.v1");
        assertThat(published.key()).isEqualTo("50000000-0000-0000-0000-000000000001");

        JsonNode envelope = objectMapper.readTree(published.eventJson());
        assertThat(envelope.path("eventType").asText()).isEqualTo("reply-delivery-succeeded");
        assertThat(envelope.path("occurredAt").asText()).isEqualTo("2026-06-30T03:30:00Z");
        assertThat(envelope.path("correlationId").asText()).isEqualTo("corr-reply-1");
        assertThat(envelope.path("payload").path("providerMessageId").asText())
            .isEqualTo("simulated:50000000-0000-0000-0000-000000000001");
    }

    @Test
    void publishesFailedDeliveryResultWhenContentContainsFailMarker() throws Exception {
        ReplyRequestEvent event = event("Please [fail] this delivery");

        ReplyDeliveryService.DeliveryResult result = service.deliver(event);

        assertThat(result.topic()).isEqualTo("channel.reply-delivery-failed.v1");
        JsonNode envelope = objectMapper.readTree(publisher.events().getFirst().eventJson());
        assertThat(envelope.path("eventType").asText()).isEqualTo("reply-delivery-failed");
        assertThat(envelope.path("payload").path("failureReason").asText())
            .isEqualTo("Simulated provider delivery failure");
    }

    private ReplyRequestEvent event(String content) {
        return new ReplyRequestEvent(
            UUID.fromString("80000000-0000-0000-0000-000000000001"),
            "reply-requested",
            Instant.parse("2026-06-30T03:29:00Z"),
            "corr-reply-1",
            "inbox-service",
            new ReplyRequestPayload(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                "FACEBOOK",
                "MESSAGE",
                "local-page-id",
                "messenger:fb-user-a",
                null,
                null,
                content
            )
        );
    }

    private record PublishedEvent(String topic, String key, String eventJson) {
    }

    private static class RecordingDeliveryResultPublisher implements DeliveryResultPublisher {

        private final List<PublishedEvent> events = new ArrayList<>();

        @Override
        public void publish(String topic, String key, String eventJson) {
            events.add(new PublishedEvent(topic, key, eventJson));
        }

        List<PublishedEvent> events() {
            return events;
        }
    }
}
