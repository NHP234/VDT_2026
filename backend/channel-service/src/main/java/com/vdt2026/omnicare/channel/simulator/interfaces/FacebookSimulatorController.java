package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventPublisher;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundNormalizer;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/simulators/facebook")
@ConditionalOnProperty(name = "app.facebook.mode", havingValue = "simulator", matchIfMissing = true)
class FacebookSimulatorController {

    private final FacebookInboundNormalizer normalizer;
    private final InboundEventPublisher inboundEventPublisher;

    FacebookSimulatorController(FacebookInboundNormalizer normalizer, InboundEventPublisher inboundEventPublisher) {
        this.normalizer = normalizer;
        this.inboundEventPublisher = inboundEventPublisher;
    }

    @PostMapping("/events")
    FacebookSimulatorResponse simulateInboundEvent(
        @Valid @RequestBody FacebookSimulatorEventRequest request,
        @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        String effectiveCorrelationId = StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(request.toCommand(), effectiveCorrelationId);
        inboundEventPublisher.publish(event);
        return new FacebookSimulatorResponse(normalizer.topic(), true, event);
    }
}
