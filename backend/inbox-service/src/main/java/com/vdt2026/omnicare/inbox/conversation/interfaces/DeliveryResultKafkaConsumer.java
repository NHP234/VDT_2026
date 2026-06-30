package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.conversation.application.DeliveryResultEvent;
import com.vdt2026.omnicare.inbox.conversation.application.DeliveryResultIngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
class DeliveryResultKafkaConsumer {

    static final String SUCCEEDED_TOPIC = "channel.reply-delivery-succeeded.v1";
    static final String FAILED_TOPIC = "channel.reply-delivery-failed.v1";

    private final ObjectMapper objectMapper;
    private final DeliveryResultIngestionService ingestionService;

    DeliveryResultKafkaConsumer(ObjectMapper objectMapper, DeliveryResultIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = {SUCCEEDED_TOPIC, FAILED_TOPIC}, groupId = "${spring.kafka.consumer.group-id}")
    void consume(String eventJson) throws JsonProcessingException {
        DeliveryResultEvent event = objectMapper.readValue(eventJson, DeliveryResultEvent.class);
        ingestionService.ingest(event);
    }
}
