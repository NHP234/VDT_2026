package com.vdt2026.omnicare.channel.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MetricsConfig {

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
