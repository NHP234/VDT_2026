package com.vdt2026.omnicare.inbox.shared.infrastructure.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class KafkaOutboxEventPublisherTests {

    @Test
    void publishesOutboxEventToKafkaTopic() {
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();
        KafkaOutboxEventPublisher publisher = new KafkaOutboxEventPublisher(kafkaTemplate);

        publisher.publish("inbox.reply-requested.v1", "message-1", "{\"eventType\":\"reply-requested\"}");

        verify(kafkaTemplate).send("inbox.reply-requested.v1", "message-1", "{\"eventType\":\"reply-requested\"}");
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> mockKafkaTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send("inbox.reply-requested.v1", "message-1", "{\"eventType\":\"reply-requested\"}"))
            .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        return kafkaTemplate;
    }
}
