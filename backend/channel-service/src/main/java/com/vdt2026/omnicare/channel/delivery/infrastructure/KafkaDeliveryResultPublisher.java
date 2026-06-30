package com.vdt2026.omnicare.channel.delivery.infrastructure;

import com.vdt2026.omnicare.channel.delivery.application.DeliveryResultPublisher;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaDeliveryResultPublisher implements DeliveryResultPublisher {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafkaTemplate;

    KafkaDeliveryResultPublisher(@Qualifier("deliveryResultKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String key, String eventJson) {
        try {
            kafkaTemplate.send(topic, key, eventJson).get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing delivery result to Kafka", ex);
        }
        catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("Could not publish delivery result to Kafka topic " + topic, ex);
        }
    }
}
