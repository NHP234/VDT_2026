package com.vdt2026.omnicare.channel.delivery.infrastructure;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaDeliveryResultPublisherTests {

    @Test
    void publishesDeliveryResultToKafkaTopic() {
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();
        KafkaDeliveryResultPublisher publisher = new KafkaDeliveryResultPublisher(kafkaTemplate);

        publisher.publish("channel.reply-delivery-succeeded.v1", "message-1", "{\"eventType\":\"reply-delivery-succeeded\"}");

        verify(kafkaTemplate).send(
            "channel.reply-delivery-succeeded.v1",
            "message-1",
            "{\"eventType\":\"reply-delivery-succeeded\"}"
        );
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> mockKafkaTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
            "channel.reply-delivery-succeeded.v1",
            "message-1",
            "{\"eventType\":\"reply-delivery-succeeded\"}"
        )).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        return kafkaTemplate;
    }
}
