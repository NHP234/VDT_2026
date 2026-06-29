package com.vdt2026.omnicare.inbox.identity.application;

import java.util.UUID;

public record AuthenticatedAgent(
    UUID id,
    String email,
    String displayName
) {
}
