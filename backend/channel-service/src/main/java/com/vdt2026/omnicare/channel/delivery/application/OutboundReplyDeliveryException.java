package com.vdt2026.omnicare.channel.delivery.application;

public class OutboundReplyDeliveryException extends RuntimeException {

    private final String providerMessageId;

    public OutboundReplyDeliveryException(String message) {
        this(message, null, null);
    }

    public OutboundReplyDeliveryException(String message, String providerMessageId, Throwable cause) {
        super(message, cause);
        this.providerMessageId = providerMessageId;
    }

    public String providerMessageId() {
        return providerMessageId;
    }
}
