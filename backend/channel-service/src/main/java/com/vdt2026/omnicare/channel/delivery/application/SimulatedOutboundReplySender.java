package com.vdt2026.omnicare.channel.delivery.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.facebook.mode", havingValue = "simulator", matchIfMissing = true)
class SimulatedOutboundReplySender implements OutboundReplySender {

    @Override
    public OutboundReplyResult send(ReplyRequestPayload payload) {
        if (payload.content().contains("[fail]")) {
            throw new OutboundReplyDeliveryException(
                "Simulated provider delivery failure",
                "simulated:" + payload.messageId(),
                null
            );
        }

        return new OutboundReplyResult("simulated:" + payload.messageId());
    }
}
