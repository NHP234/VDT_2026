package com.vdt2026.omnicare.channel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

class KafkaConsumerConfigTests {

    @Test
    void retryBackOffUsesBoundedRetriesAfterInitialAttempt() {
        var backOff = KafkaConsumerConfig.retryBackOff(3, 250L);

        assertThat(backOff.getInterval()).isEqualTo(250L);
        assertThat(backOff.getMaxAttempts()).isEqualTo(2L);
    }

    @Test
    void retryBackOffClampsInvalidValuesToNoRetryAndNoDelay() {
        var backOff = KafkaConsumerConfig.retryBackOff(0, -100L);

        assertThat(backOff.getInterval()).isZero();
        assertThat(backOff.getMaxAttempts()).isZero();
    }

    @Test
    void deadLetterTopicAppendsDlqSuffixAndKeepsSourcePartition() {
        var record = new ConsumerRecord<>("inbox.reply-requested.v1", 2, 10L, "key", "value");

        assertThat(KafkaConsumerConfig.deadLetterTopic(record))
            .isEqualTo(new TopicPartition("inbox.reply-requested.v1.dlq", 2));
    }
}
