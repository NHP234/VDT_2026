package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.conversation.application.DeliveryResultEvent;
import com.vdt2026.omnicare.inbox.conversation.application.DeliveryResultIngestionService;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.kafka.consumers.enabled", havingValue = "true", matchIfMissing = true)
class DeliveryResultKafkaConsumer {

    static final String SUCCEEDED_TOPIC = "channel.reply-delivery-succeeded.v1";
    static final String FAILED_TOPIC = "channel.reply-delivery-failed.v1";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String TRACE_ID_KEY = "traceId";

    private final ObjectMapper objectMapper;
    private final DeliveryResultIngestionService ingestionService;

    DeliveryResultKafkaConsumer(ObjectMapper objectMapper, DeliveryResultIngestionService ingestionService) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @KafkaListener(topics = {SUCCEEDED_TOPIC, FAILED_TOPIC}, groupId = "${spring.kafka.consumer.group-id}")
    void consume(String eventJson) throws JsonProcessingException {
        DeliveryResultEvent event = objectMapper.readValue(eventJson, DeliveryResultEvent.class);
        withCorrelation(event.correlationId(), () -> ingestionService.ingest(event));
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
