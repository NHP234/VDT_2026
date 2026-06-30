package com.vdt2026.omnicare.channel.facebook.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FacebookWebhookSignatureValidator {

    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String appSecret;
    private final boolean required;

    public FacebookWebhookSignatureValidator(
        @Value("${app.facebook.app-secret:}") String appSecret,
        @Value("${app.facebook.mode:simulator}") String mode
    ) {
        this.appSecret = appSecret;
        this.required = !"simulator".equalsIgnoreCase(mode);
    }

    public void validate(String rawBody, String signatureHeader) {
        if (!required) {
            return;
        }
        if (!StringUtils.hasText(appSecret)) {
            throw new FacebookWebhookSignatureException("Facebook app secret is not configured");
        }
        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            throw new FacebookWebhookSignatureException("Missing or unsupported Facebook signature");
        }

        String expected = SIGNATURE_PREFIX + computeSignature(rawBody == null ? "" : rawBody);
        if (!constantTimeEquals(expected, signatureHeader)) {
            throw new FacebookWebhookSignatureException("Facebook webhook signature mismatch");
        }
    }

    private String computeSignature(String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate Facebook webhook signature", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
