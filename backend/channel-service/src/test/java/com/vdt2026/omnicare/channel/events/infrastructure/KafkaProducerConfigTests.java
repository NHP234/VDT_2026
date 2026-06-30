package com.vdt2026.omnicare.channel.events.infrastructure;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

class KafkaProducerConfigTests {

    @Test
    void serializesEventTimestampsAsIso8601Strings() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setBootstrapServers(List.of("localhost:9092"));
        KafkaProducerConfig config = new KafkaProducerConfig();

        ProducerFactory<String, EventEnvelope<NormalizedInboundMessagePayload>> producerFactory =
            config.inboundEventProducerFactory(kafkaProperties, JsonMapper.builder().findAndAddModules().build());

        @SuppressWarnings("unchecked")
        Serializer<EventEnvelope<NormalizedInboundMessagePayload>> valueSerializer =
            ((DefaultKafkaProducerFactory<String, EventEnvelope<NormalizedInboundMessagePayload>>) producerFactory)
                .getValueSerializer();
        String json = new String(valueSerializer.serialize("inbox.message-received.v1", event()), UTF_8);

        assertThat(json).contains("\"occurredAt\":\"2026-06-30T02:25:00Z\"");
    }

    private EventEnvelope<NormalizedInboundMessagePayload> event() {
        Instant occurredAt = Instant.parse("2026-06-30T02:25:00Z");
        NormalizedInboundMessagePayload payload = new NormalizedInboundMessagePayload(
            "FACEBOOK",
            "MESSAGE",
            "local-page-id",
            "facebook:messenger:local-page-id:fb-user-c",
            "mid.local.facebook.messenger.1001",
            "fb-user-c",
            "Le Van C",
            null,
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
