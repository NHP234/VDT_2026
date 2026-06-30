package com.vdt2026.omnicare.channel.events.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InboundEventDispatchServiceTests {

    @Test
    void publishesAcceptedEvent() {
        RecordingPublisher publisher = new RecordingPublisher();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        InboundEventDispatchService service = new InboundEventDispatchService(event -> true, publisher, meterRegistry);
        EventEnvelope<NormalizedInboundMessagePayload> event = event("mid-accepted");

        InboundEventDispatchService.DispatchResult result = service.dispatch(event);

        assertThat(result).isEqualTo(InboundEventDispatchService.DispatchResult.PUBLISHED);
        assertThat(publisher.events()).containsExactly(event);
        assertThat(meterRegistry.get("omnicare.inbound.events")
            .tag("result", "published")
            .tag("channel", "FACEBOOK")
            .tag("sourceType", "MESSAGE")
            .counter()
            .count()).isEqualTo(1.0);
    }

    @Test
    void doesNotPublishDuplicateEvent() {
        RecordingPublisher publisher = new RecordingPublisher();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        InboundEventDispatchService service = new InboundEventDispatchService(event -> false, publisher, meterRegistry);
        EventEnvelope<NormalizedInboundMessagePayload> event = event("mid-duplicate");

        InboundEventDispatchService.DispatchResult result = service.dispatch(event);

        assertThat(result).isEqualTo(InboundEventDispatchService.DispatchResult.DUPLICATE);
        assertThat(publisher.events()).isEmpty();
        assertThat(meterRegistry.get("omnicare.inbound.events")
            .tag("result", "duplicate")
            .tag("channel", "FACEBOOK")
            .tag("sourceType", "MESSAGE")
            .counter()
            .count()).isEqualTo(1.0);
    }

    private EventEnvelope<NormalizedInboundMessagePayload> event(String externalMessageId) {
        Instant occurredAt = Instant.parse("2026-06-30T00:00:00Z");
        return new EventEnvelope<>(
            UUID.randomUUID(),
            "message-received",
            occurredAt,
            "corr-test",
            "channel-service.facebook-simulator",
            new NormalizedInboundMessagePayload(
                "FACEBOOK",
                "MESSAGE",
                "local-page-id",
                "facebook:messenger:local-page-id:fb-user",
                externalMessageId,
                "fb-user",
                "Facebook User",
                null,
                "hello",
                occurredAt
            )
        );
    }

    private static class RecordingPublisher implements InboundEventPublisher {

        private final List<EventEnvelope<NormalizedInboundMessagePayload>> events = new ArrayList<>();

        @Override
        public void publish(EventEnvelope<NormalizedInboundMessagePayload> event) {
            events.add(event);
        }

        List<EventEnvelope<NormalizedInboundMessagePayload>> events() {
            return events;
        }
    }
}
