package com.vdt2026.omnicare.channel.delivery.application;

public interface DeliveryResultPublisher {

    void publish(String topic, String key, String eventJson);
}
