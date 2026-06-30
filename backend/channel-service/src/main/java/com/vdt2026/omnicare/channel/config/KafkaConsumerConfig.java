package com.vdt2026.omnicare.channel.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
class KafkaConsumerConfig {

    @Bean
    CommonErrorHandler kafkaErrorHandler(
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${app.kafka.retry.max-attempts:3}") int maxAttempts,
        @Value("${app.kafka.retry.backoff-ms:1000}") long backoffMs
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, exception) -> deadLetterTopic(record)
        );
        recoverer.setFailIfSendResultIsError(true);
        return new DefaultErrorHandler(recoverer, retryBackOff(maxAttempts, backoffMs));
    }

    static FixedBackOff retryBackOff(int maxAttempts, long backoffMs) {
        long retriesAfterInitialAttempt = Math.max(1, maxAttempts) - 1L;
        return new FixedBackOff(Math.max(0L, backoffMs), retriesAfterInitialAttempt);
    }

    static TopicPartition deadLetterTopic(ConsumerRecord<?, ?> record) {
        return new TopicPartition(record.topic() + ".dlq", record.partition());
    }
}
