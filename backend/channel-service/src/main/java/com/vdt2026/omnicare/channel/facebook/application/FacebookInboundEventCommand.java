package com.vdt2026.omnicare.channel.facebook.application;

import java.time.Instant;

public record FacebookInboundEventCommand(
    FacebookInboundEventType type,
    String pageId,
    String senderId,
    String senderDisplayName,
    String commenterId,
    String commenterDisplayName,
    String messageId,
    String postId,
    String commentId,
    String rootCommentId,
    String content,
    Instant occurredAt
) {
}
