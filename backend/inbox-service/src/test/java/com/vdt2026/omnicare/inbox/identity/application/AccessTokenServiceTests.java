package com.vdt2026.omnicare.inbox.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessTokenServiceTests {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
    private final AccessTokenService tokenService = new AccessTokenService(
        new TokenProperties("test-secret-with-enough-length", 60),
        new ObjectMapper(),
        clock
    );

    @Test
    void issuedTokenCanBeVerified() {
        AuthenticatedAgent agent = new AuthenticatedAgent(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "agent@example.test",
            "Demo Agent"
        );

        AccessTokenService.IssuedToken token = tokenService.issue(agent);

        assertThat(tokenService.verify(token.value())).contains(agent);
    }

    @Test
    void tamperedTokenIsRejected() {
        AuthenticatedAgent agent = new AuthenticatedAgent(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "agent@example.test",
            "Demo Agent"
        );
        String token = tokenService.issue(agent).value();

        assertThat(tokenService.verify(token + "x")).isEmpty();
    }
}
