package com.vdt2026.omnicare.inbox.shared.application;

public interface OutboxEventPublisher {

    void publish(String topic, String key, String eventJson);
}
