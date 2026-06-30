package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.email.application.EmailInboundEventCommand;
import java.time.Instant;
import java.util.List;
import org.springframework.util.StringUtils;

record EmailSimulatorEventRequest(
    String providerAccountId,
    String fromEmail,
    String fromDisplayName,
    String toEmail,
    String messageId,
    String subject,
    String textContent,
    Instant occurredAt,
    String inReplyTo,
    List<String> references
) {

    EmailInboundEventCommand toCommand(String defaultProviderAccountId) {
        String effectiveProviderAccountId = StringUtils.hasText(providerAccountId)
            ? providerAccountId
            : defaultProviderAccountId;
        return new EmailInboundEventCommand(
            effectiveProviderAccountId,
            fromEmail,
            fromDisplayName,
            toEmail,
            messageId,
            subject,
            textContent,
            occurredAt,
            inReplyTo,
            references
        );
    }
}
