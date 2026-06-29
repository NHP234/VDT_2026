package com.vdt2026.omnicare.inbox.identity.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final TokenProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public AccessTokenService(TokenProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AccessTokenService(TokenProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public IssuedToken issue(AuthenticatedAgent agent) {
        Instant expiresAt = Instant.now(clock).plusSeconds(properties.ttlSeconds());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", agent.id().toString());
        claims.put("email", agent.email());
        claims.put("displayName", agent.displayName());
        claims.put("exp", expiresAt.getEpochSecond());

        String header = encodeJson(Map.of("alg", "HS256", "typ", "OMNICARE"));
        String payload = encodeJson(claims);
        String signature = sign(header + "." + payload);
        return new IssuedToken(header + "." + payload + "." + signature, expiresAt, properties.ttlSeconds());
    }

    public Optional<AuthenticatedAgent> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return Optional.empty();
            }

            Map<String, Object> claims = objectMapper.readValue(decode(parts[1]), MAP_TYPE);
            long expiresAt = ((Number) claims.get("exp")).longValue();
            if (Instant.now(clock).getEpochSecond() >= expiresAt) {
                return Optional.empty();
            }

            return Optional.of(new AuthenticatedAgent(
                UUID.fromString((String) claims.get("sub")),
                (String) claims.get("email"),
                (String) claims.get("displayName")
            ));
        }
        catch (RuntimeException | java.io.IOException ex) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encode(objectMapper.writeValueAsBytes(value));
        }
        catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not encode access token", ex);
        }
    }

    private String sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return encode(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        }
        catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Could not sign access token", ex);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public record IssuedToken(
        String value,
        Instant expiresAt,
        long expiresInSeconds
    ) {
    }
}
