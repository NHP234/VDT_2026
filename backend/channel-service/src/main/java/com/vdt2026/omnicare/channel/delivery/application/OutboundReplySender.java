package com.vdt2026.omnicare.channel.delivery.application;

public interface OutboundReplySender {

    boolean supports(ReplyRequestPayload payload);

    OutboundReplyResult send(ReplyRequestPayload payload);
}
