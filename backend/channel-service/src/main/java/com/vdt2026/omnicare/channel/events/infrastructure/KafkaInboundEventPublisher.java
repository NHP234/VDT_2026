package com.vdt2026.omnicare.channel.events.infrastructure;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventPublisher;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaInboundEventPublisher implements InboundEventPublisher {

    private static final String TOPIC = "inbox.message-received.v1";

    private final KafkaTemplate<String, EventEnvelope<NormalizedInboundMessagePayload>> kafkaTemplate;

    KafkaInboundEventPublisher(
        @Qualifier("inboundEventKafkaTemplate") KafkaTemplate<String, EventEnvelope<NormalizedInboundMessagePayload>> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(EventEnvelope<NormalizedInboundMessagePayload> event) {
        kafkaTemplate.send(TOPIC, event.payload().externalConversationId(), event);
        kafkaTemplate.flush();
    }
}
