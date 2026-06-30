package com.vdt2026.omnicare.channel.delivery.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.channel.delivery.application.ReplyDeliveryService;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestEvent;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
class ReplyRequestKafkaConsumer {

    static final String REPLY_REQUESTED_TOPIC = "inbox.reply-requested.v1";
    static final String REPLY_RETRY_REQUESTED_TOPIC = "inbox.reply-retry-requested.v1";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String TRACE_ID_KEY = "traceId";

    private final ObjectMapper objectMapper;
    private final ReplyDeliveryService deliveryService;

    ReplyRequestKafkaConsumer(ObjectMapper objectMapper, ReplyDeliveryService deliveryService) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(
        topics = {REPLY_REQUESTED_TOPIC, REPLY_RETRY_REQUESTED_TOPIC},
        groupId = "${spring.kafka.consumer.group-id}"
    )
    void consume(String eventJson) throws JsonProcessingException {
        ReplyRequestEvent event = objectMapper.readValue(eventJson, ReplyRequestEvent.class);
        withCorrelation(event.correlationId(), () -> deliveryService.deliver(event));
    }

    private void withCorrelation(String correlationId, Runnable action) {
        if (!StringUtils.hasText(correlationId)) {
            action.run();
            return;
        }
        MDC.put(CORRELATION_ID_KEY, correlationId);
        MDC.put(TRACE_ID_KEY, correlationId);
        try {
            action.run();
        }
        finally {
            MDC.remove(CORRELATION_ID_KEY);
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
