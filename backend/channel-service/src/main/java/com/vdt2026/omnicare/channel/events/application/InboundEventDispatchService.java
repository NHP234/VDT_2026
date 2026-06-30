package com.vdt2026.omnicare.channel.events.application;

import org.springframework.stereotype.Service;

@Service
public class InboundEventDispatchService {

    private final InboundEventDeduplicator deduplicator;
    private final InboundEventPublisher publisher;

    public InboundEventDispatchService(InboundEventDeduplicator deduplicator, InboundEventPublisher publisher) {
        this.deduplicator = deduplicator;
        this.publisher = publisher;
    }

    public DispatchResult dispatch(EventEnvelope<NormalizedInboundMessagePayload> event) {
        if (!deduplicator.accept(event)) {
            return DispatchResult.DUPLICATE;
        }

        publisher.publish(event);
        return DispatchResult.PUBLISHED;
    }

    public enum DispatchResult {
        PUBLISHED,
        DUPLICATE
    }
}
