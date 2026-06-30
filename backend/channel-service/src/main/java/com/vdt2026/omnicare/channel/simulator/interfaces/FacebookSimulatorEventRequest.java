package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundEventCommand;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

record FacebookSimulatorEventRequest(
    @NotNull FacebookInboundEventType type,
    @NotBlank String pageId,
    String senderId,
    String senderDisplayName,
    String commenterId,
    String commenterDisplayName,
    String messageId,
    String postId,
    String commentId,
    String rootCommentId,
    @NotBlank String content,
    Instant occurredAt
) {

    FacebookInboundEventCommand toCommand() {
        return new FacebookInboundEventCommand(
            type,
            pageId,
            senderId,
            senderDisplayName,
            commenterId,
            commenterDisplayName,
            messageId,
            postId,
            commentId,
            rootCommentId,
            content,
            occurredAt
        );
    }
}
