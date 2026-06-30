package com.vdt2026.omnicare.channel.facebook.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class FacebookWebhookSignatureValidatorTests {

    @Test
    void acceptsValidSignatureInRealMode() {
        String body = "{\"object\":\"page\"}";
        FacebookWebhookSignatureValidator validator = new FacebookWebhookSignatureValidator("app-secret", "real");

        assertThatCode(() -> validator.validate(body, signature("app-secret", body)))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidSignatureInRealMode() {
        FacebookWebhookSignatureValidator validator = new FacebookWebhookSignatureValidator("app-secret", "real");

        assertThatThrownBy(() -> validator.validate("{\"object\":\"page\"}", "sha256=bad"))
            .isInstanceOf(FacebookWebhookSignatureException.class)
            .hasMessage("Facebook webhook signature mismatch");
    }

    @Test
    void rejectsMissingSecretInRealMode() {
        FacebookWebhookSignatureValidator validator = new FacebookWebhookSignatureValidator("", "real");

        assertThatThrownBy(() -> validator.validate("{\"object\":\"page\"}", "sha256=bad"))
            .isInstanceOf(FacebookWebhookSignatureException.class)
            .hasMessage("Facebook app secret is not configured");
    }

    @Test
    void skipsSignatureValidationInSimulatorMode() {
        FacebookWebhookSignatureValidator validator = new FacebookWebhookSignatureValidator("", "simulator");

        assertThatCode(() -> validator.validate("{\"object\":\"page\"}", null))
            .doesNotThrowAnyException();
    }

    static String signature(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
