package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;

record FacebookSimulatorResponse(
    String topic,
    boolean published,
    EventEnvelope<NormalizedInboundMessagePayload> event
) {
}
