package com.vdt2026.omnicare.inbox.identity.interfaces;

import com.vdt2026.omnicare.inbox.identity.application.AuthService;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    AgentResponse agent
) {

    static LoginResponse from(AuthService.LoginResult result) {
        return new LoginResponse(
            result.accessToken(),
            "Bearer",
            result.expiresInSeconds(),
            AgentResponse.from(result.agent())
        );
    }
}
