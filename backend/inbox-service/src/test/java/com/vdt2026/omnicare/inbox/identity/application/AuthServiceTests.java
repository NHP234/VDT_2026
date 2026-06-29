package com.vdt2026.omnicare.inbox.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTests {

    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final AccessTokenService accessTokenService = new AccessTokenService(
        new TokenProperties("test-secret-with-enough-length", 60),
        new ObjectMapper(),
        Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC)
    );
    private final AuthService authService = new AuthService(agentRepository, passwordEncoder, accessTokenService);

    @Test
    void validCredentialsReturnAccessToken() {
        AgentEntity agent = new AgentEntity(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "agent@example.test",
            "Demo Agent",
            passwordEncoder.encode("change-me-local-only"),
            true,
            Instant.parse("2026-06-22T00:00:00Z")
        );
        when(agentRepository.findByEmailIgnoreCase("agent@example.test")).thenReturn(Optional.of(agent));

        AuthService.LoginResult result = authService.login("agent@example.test", "change-me-local-only");

        assertThat(result.agent().email()).isEqualTo("agent@example.test");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.expiresInSeconds()).isEqualTo(60);
    }

    @Test
    void invalidPasswordIsRejected() {
        AgentEntity agent = new AgentEntity(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "agent@example.test",
            "Demo Agent",
            passwordEncoder.encode("change-me-local-only"),
            true,
            Instant.parse("2026-06-22T00:00:00Z")
        );
        when(agentRepository.findByEmailIgnoreCase("agent@example.test")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> authService.login("agent@example.test", "wrong-password"))
            .isInstanceOf(AuthenticationException.class);
    }
}
