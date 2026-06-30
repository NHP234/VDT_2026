package com.vdt2026.omnicare.inbox.shared.infrastructure.kafka;

import com.vdt2026.omnicare.inbox.shared.application.OutboxEventPublisher;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafkaTemplate;

    KafkaOutboxEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String key, String eventJson) {
        try {
            kafkaTemplate.send(topic, key, eventJson).get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing outbox event to Kafka", ex);
        }
        catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Could not publish outbox event to Kafka topic " + topic, ex);
        }
    }
}
