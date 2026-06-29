package com.vdt2026.omnicare.inbox.identity.application;

import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;

    public AuthService(AgentRepository agentRepository, PasswordEncoder passwordEncoder, AccessTokenService accessTokenService) {
        this.agentRepository = agentRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
    }

    public LoginResult login(String email, String password) {
        AgentEntity agent = agentRepository.findByEmailIgnoreCase(email)
            .filter(AgentEntity::active)
            .filter(candidate -> passwordEncoder.matches(password, candidate.passwordHash()))
            .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        AuthenticatedAgent authenticatedAgent = new AuthenticatedAgent(agent.id(), agent.email(), agent.displayName());
        AccessTokenService.IssuedToken token = accessTokenService.issue(authenticatedAgent);
        return new LoginResult(authenticatedAgent, token.value(), token.expiresInSeconds());
    }

    public record LoginResult(
        AuthenticatedAgent agent,
        String accessToken,
        long expiresInSeconds
    ) {
    }
}
