package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.conversation.application.InboundMessageIngestionService;
import com.vdt2026.omnicare.inbox.conversation.application.InboundMessageReceivedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
class InboundMessageKafkaConsumer {

    static final String TOPIC = "inbox.message-received.v1";

    private final ObjectMapper objectMapper;
    private final InboundMessageIngestionService ingestionService;

    InboundMessageKafkaConsumer(ObjectMapper objectMapper, InboundMessageIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    void consume(String eventJson) throws JsonProcessingException {
        InboundMessageReceivedEvent event = objectMapper.readValue(eventJson, InboundMessageReceivedEvent.class);
        ingestionService.ingest(event);
    }
}
