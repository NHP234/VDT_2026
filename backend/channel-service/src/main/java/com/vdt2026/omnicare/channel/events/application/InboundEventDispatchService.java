package com.vdt2026.omnicare.channel.events.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InboundEventDispatchService {

    private static final String INBOUND_EVENTS_METRIC = "omnicare.inbound.events";

    private final InboundEventDeduplicator deduplicator;
    private final InboundEventPublisher publisher;
    private final MeterRegistry meterRegistry;

    InboundEventDispatchService(InboundEventDeduplicator deduplicator, InboundEventPublisher publisher) {
        this(deduplicator, publisher, Metrics.globalRegistry);
    }

    @Autowired
    public InboundEventDispatchService(
        InboundEventDeduplicator deduplicator,
        InboundEventPublisher publisher,
        ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        this(deduplicator, publisher, meterRegistryProvider.getIfAvailable(() -> Metrics.globalRegistry));
    }

    InboundEventDispatchService(
        InboundEventDeduplicator deduplicator,
        InboundEventPublisher publisher,
        MeterRegistry meterRegistry
    ) {
        this.deduplicator = deduplicator;
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
    }

    public DispatchResult dispatch(EventEnvelope<NormalizedInboundMessagePayload> event) {
        if (!deduplicator.accept(event)) {
            incrementInboundMetric("duplicate", event);
            return DispatchResult.DUPLICATE;
        }

        publisher.publish(event);
        incrementInboundMetric("published", event);
        return DispatchResult.PUBLISHED;
    }

    private void incrementInboundMetric(String result, EventEnvelope<NormalizedInboundMessagePayload> event) {
        NormalizedInboundMessagePayload payload = event.payload();
        Counter.builder(INBOUND_EVENTS_METRIC)
            .description("Inbound channel events accepted or rejected before Kafka publication")
            .tag("result", result)
            .tag("channel", tagValue(payload.channel()))
            .tag("sourceType", tagValue(payload.sourceType()))
            .register(meterRegistry)
            .increment();
    }

    private String tagValue(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    public enum DispatchResult {
        PUBLISHED,
        DUPLICATE
    }
}
