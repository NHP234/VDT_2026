package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.email.application.EmailInboundNormalizer;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventDispatchService;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulators/email")
@ConditionalOnProperty(name = "app.email.mode", havingValue = "simulator", matchIfMissing = true)
class EmailSimulatorController {

    private final EmailInboundNormalizer normalizer;
    private final InboundEventDispatchService dispatchService;
    private final String defaultProviderAccountId;

    EmailSimulatorController(
        EmailInboundNormalizer normalizer,
        InboundEventDispatchService dispatchService,
        @Value("${app.email.mailbox:demo@example.test}") String defaultProviderAccountId
    ) {
        this.normalizer = normalizer;
        this.dispatchService = dispatchService;
        this.defaultProviderAccountId = defaultProviderAccountId;
    }

    @PostMapping("/events")
    FacebookSimulatorResponse simulateInboundEvent(
        @Valid @RequestBody EmailSimulatorEventRequest request,
        @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        String effectiveCorrelationId = StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(
            request.toCommand(defaultProviderAccountId),
            effectiveCorrelationId
        );
        InboundEventDispatchService.DispatchResult result = dispatchService.dispatch(event);
        return new FacebookSimulatorResponse(normalizer.topic(), result == InboundEventDispatchService.DispatchResult.PUBLISHED, event);
    }
}
