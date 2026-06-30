package com.vdt2026.omnicare.channel.events.application;

public interface InboundEventPublisher {

    void publish(EventEnvelope<NormalizedInboundMessagePayload> event);
}
