package com.vdt2026.omnicare.channel.facebook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FacebookWebhookVerificationServiceTests {

    @Test
    void returnsChallengeWhenModeAndTokenMatch() {
        FacebookWebhookVerificationService service = new FacebookWebhookVerificationService("verify-me");

        String challenge = service.verify("subscribe", "verify-me", "123456789");

        assertThat(challenge).isEqualTo("123456789");
    }

    @Test
    void rejectsInvalidToken() {
        FacebookWebhookVerificationService service = new FacebookWebhookVerificationService("verify-me");

        assertThatThrownBy(() -> service.verify("subscribe", "wrong-token", "123456789"))
            .isInstanceOf(FacebookWebhookVerificationException.class)
            .hasMessage("Facebook webhook verification failed");
    }

    @Test
    void rejectsMissingConfiguredToken() {
        FacebookWebhookVerificationService service = new FacebookWebhookVerificationService("");

        assertThatThrownBy(() -> service.verify("subscribe", "verify-me", "123456789"))
            .isInstanceOf(FacebookWebhookVerificationException.class)
            .hasMessage("Facebook verify token is not configured");
    }
}
