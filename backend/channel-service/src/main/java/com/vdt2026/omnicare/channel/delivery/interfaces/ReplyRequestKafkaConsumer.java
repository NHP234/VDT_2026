package com.vdt2026.omnicare.channel.delivery.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestEvent;
import com.vdt2026.omnicare.channel.delivery.application.SimulatedReplyDeliveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ReplyRequestKafkaConsumer {

    static final String REPLY_REQUESTED_TOPIC = "inbox.reply-requested.v1";
    static final String REPLY_RETRY_REQUESTED_TOPIC = "inbox.reply-retry-requested.v1";

    private final ObjectMapper objectMapper;
    private final SimulatedReplyDeliveryService deliveryService;

    ReplyRequestKafkaConsumer(ObjectMapper objectMapper, SimulatedReplyDeliveryService deliveryService) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
        topics = {REPLY_REQUESTED_TOPIC, REPLY_RETRY_REQUESTED_TOPIC},
        groupId = "${spring.kafka.consumer.group-id}"
    )
    void consume(String eventJson) throws JsonProcessingException {
        ReplyRequestEvent event = objectMapper.readValue(eventJson, ReplyRequestEvent.class);
        deliveryService.deliver(event);
    }
}
