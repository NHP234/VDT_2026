package com.vdt2026.omnicare.channel.events.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
class KafkaProducerConfig {

    @Bean
    ProducerFactory<String, EventEnvelope<NormalizedInboundMessagePayload>> inboundEventProducerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper
    ) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        JsonSerializer<EventEnvelope<NormalizedInboundMessagePayload>> valueSerializer = new JsonSerializer<>(
            objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        );
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(
            producerProperties,
            new StringSerializer(),
            valueSerializer
        );
    }

    @Bean
    KafkaTemplate<String, EventEnvelope<NormalizedInboundMessagePayload>> inboundEventKafkaTemplate(
        ProducerFactory<String, EventEnvelope<NormalizedInboundMessagePayload>> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    KafkaTemplate<String, String> deliveryResultKafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
            kafkaProperties.buildProducerProperties(),
            new StringSerializer(),
            new StringSerializer()
        ));
    }
}
