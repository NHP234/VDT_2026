package com.vdt2026.omnicare.inbox.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.token")
public record TokenProperties(
    String secret,
    long ttlSeconds
) {
}
