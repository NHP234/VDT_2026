package com.vdt2026.omnicare.channel.events.application;

public interface InboundEventDeduplicator {

    boolean accept(EventEnvelope<NormalizedInboundMessagePayload> event);
}
