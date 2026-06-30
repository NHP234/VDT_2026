package com.vdt2026.omnicare.channel.facebook.application;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FacebookInboundNormalizer {

    static final String TOPIC = "inbox.message-received.v1";

    private final Clock clock;

    public FacebookInboundNormalizer() {
        this(Clock.systemUTC());
    }

    FacebookInboundNormalizer(Clock clock) {
        this.clock = clock;
    }

    public EventEnvelope<NormalizedInboundMessagePayload> normalize(FacebookInboundEventCommand command, String correlationId) {
        require(command.type() != null, "Facebook event type is required");
        requireHasText(command.pageId(), "Facebook pageId is required");
        requireHasText(command.content(), "Facebook text content is required");

        Instant occurredAt = command.occurredAt() == null ? Instant.now(clock) : command.occurredAt();
        String normalizedCorrelationId = StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();

        NormalizedInboundMessagePayload payload = switch (command.type()) {
            case MESSENGER_MESSAGE -> messengerPayload(command, occurredAt);
            case PAGE_COMMENT -> commentPayload(command, occurredAt);
        };

        return new EventEnvelope<>(
            UUID.randomUUID(),
            "message-received",
            occurredAt,
            normalizedCorrelationId,
            "channel-service.facebook-simulator",
            payload
        );
    }

    public String topic() {
        return TOPIC;
    }

    private NormalizedInboundMessagePayload messengerPayload(FacebookInboundEventCommand command, Instant occurredAt) {
        requireHasText(command.senderId(), "Messenger senderId is required");
        requireHasText(command.messageId(), "Messenger messageId is required");

        return new NormalizedInboundMessagePayload(
            "FACEBOOK",
            "MESSAGE",
            command.pageId(),
            "facebook:messenger:%s:%s".formatted(command.pageId(), command.senderId()),
            command.messageId(),
            command.senderId(),
            defaultDisplayName(command.senderDisplayName(), command.senderId()),
            command.content(),
            occurredAt
        );
    }

    private NormalizedInboundMessagePayload commentPayload(FacebookInboundEventCommand command, Instant occurredAt) {
        requireHasText(command.commenterId(), "Comment commenterId is required");
        requireHasText(command.postId(), "Comment postId is required");
        requireHasText(command.commentId(), "Comment commentId is required");

        String rootCommentId = StringUtils.hasText(command.rootCommentId()) ? command.rootCommentId() : command.commentId();

        return new NormalizedInboundMessagePayload(
            "FACEBOOK",
            "COMMENT",
            command.pageId(),
            "facebook:comment:%s:%s:%s".formatted(command.pageId(), command.postId(), rootCommentId),
            command.commentId(),
            command.commenterId(),
            defaultDisplayName(command.commenterDisplayName(), command.commenterId()),
            command.content(),
            occurredAt
        );
    }

    private static String defaultDisplayName(String displayName, String fallbackId) {
        return StringUtils.hasText(displayName) ? displayName : fallbackId;
    }

    private static void requireHasText(String value, String message) {
        require(StringUtils.hasText(value), message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidFacebookInboundEventException(message);
        }
    }
}
