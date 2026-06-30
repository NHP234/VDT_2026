package com.vdt2026.omnicare.channel.facebook.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FacebookWebhookVerificationService {

    private final String verifyToken;

    public FacebookWebhookVerificationService(@Value("${app.facebook.verify-token:}") String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String verify(String mode, String token, String challenge) {
        if (!StringUtils.hasText(verifyToken)) {
            throw new FacebookWebhookVerificationException("Facebook verify token is not configured");
        }
        if (!"subscribe".equals(mode) || !verifyToken.equals(token) || !StringUtils.hasText(challenge)) {
            throw new FacebookWebhookVerificationException("Facebook webhook verification failed");
        }
        return challenge;
    }
}
