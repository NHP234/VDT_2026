package com.vdt2026.omnicare.channel.email.application;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailInboundNormalizer {

    static final String TOPIC = "inbox.message-received.v1";
    private static final int MAX_CONTENT_LENGTH = 10_000;
    private static final int MAX_SUBJECT_LENGTH = 300;

    private final Clock clock;

    public EmailInboundNormalizer() {
        this(Clock.systemUTC());
    }

    EmailInboundNormalizer(Clock clock) {
        this.clock = clock;
    }

    public EventEnvelope<NormalizedInboundMessagePayload> normalize(EmailInboundEventCommand command, String correlationId) {
        requireHasText(command.providerAccountId(), "Email providerAccountId is required");
        requireHasText(command.fromEmail(), "Email fromEmail is required");
        requireHasText(command.messageId(), "Email messageId is required");
        requireHasText(command.textContent(), "Email textContent is required");

        Instant occurredAt = command.occurredAt() == null ? Instant.now(clock) : command.occurredAt();
        String normalizedCorrelationId = StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();
        String providerAccountId = canonical(command.providerAccountId());
        String fromEmail = canonical(command.fromEmail());
        String messageId = canonicalMessageId(command.messageId());
        String threadRootMessageId = threadRootMessageId(command, messageId);

        NormalizedInboundMessagePayload payload = new NormalizedInboundMessagePayload(
            "EMAIL",
            "EMAIL",
            providerAccountId,
            "email:%s:%s".formatted(providerAccountId, threadRootMessageId),
            messageId,
            fromEmail,
            defaultDisplayName(command.fromDisplayName(), fromEmail),
            optionalLimitedText(command.subject(), MAX_SUBJECT_LENGTH),
            limitedText(command.textContent(), MAX_CONTENT_LENGTH),
            occurredAt
        );

        return new EventEnvelope<>(
            UUID.randomUUID(),
            "message-received",
            occurredAt,
            normalizedCorrelationId,
            "channel-service.email-simulator",
            payload
        );
    }

    public String topic() {
        return TOPIC;
    }

    private String threadRootMessageId(EmailInboundEventCommand command, String fallbackMessageId) {
        String firstReference = firstReference(command.references());
        if (StringUtils.hasText(firstReference)) {
            return canonicalMessageId(firstReference);
        }
        if (StringUtils.hasText(command.inReplyTo())) {
            return canonicalMessageId(command.inReplyTo());
        }
        return fallbackMessageId;
    }

    private String firstReference(List<String> references) {
        if (references == null) {
            return null;
        }
        return references.stream()
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
    }

    private String defaultDisplayName(String displayName, String fallbackEmail) {
        if (!StringUtils.hasText(displayName)) {
            return fallbackEmail;
        }
        return displayName.trim();
    }

    private String optionalLimitedText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return limitedText(value, maxLength);
    }

    private String limitedText(String value, int maxLength) {
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private String canonical(String value) {
        return value.trim().toLowerCase();
    }

    private String canonicalMessageId(String value) {
        return value.trim();
    }

    private void requireHasText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidEmailInboundEventException(message);
        }
    }
}
