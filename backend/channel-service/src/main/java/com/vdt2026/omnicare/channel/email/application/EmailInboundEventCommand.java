package com.vdt2026.omnicare.channel.email.application;

import java.time.Instant;
import java.util.List;

public record EmailInboundEventCommand(
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
}
