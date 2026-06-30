package com.vdt2026.omnicare.channel.delivery.application;

public interface OutboundReplySender {

    OutboundReplyResult send(ReplyRequestPayload payload);
}
