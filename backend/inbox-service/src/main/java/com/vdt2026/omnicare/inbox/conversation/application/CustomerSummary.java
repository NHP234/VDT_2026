package com.vdt2026.omnicare.inbox.conversation.application;

import java.util.UUID;

public record CustomerSummary(
    UUID id,
    String displayName
) {
}
