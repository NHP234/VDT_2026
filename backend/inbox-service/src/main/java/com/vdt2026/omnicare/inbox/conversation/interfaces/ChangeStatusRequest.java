package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import jakarta.validation.constraints.NotNull;

record ChangeStatusRequest(
    @NotNull ConversationStatus status
) {
}
