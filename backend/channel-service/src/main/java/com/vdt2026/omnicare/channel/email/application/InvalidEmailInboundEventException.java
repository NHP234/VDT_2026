package com.vdt2026.omnicare.channel.email.application;

public class InvalidEmailInboundEventException extends RuntimeException {

    public InvalidEmailInboundEventException(String message) {
        super(message);
    }
}
