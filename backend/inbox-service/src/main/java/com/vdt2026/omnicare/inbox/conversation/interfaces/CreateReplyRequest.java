package com.vdt2026.omnicare.inbox.conversation.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateReplyRequest(
    @NotBlank @Size(max = 10000) String content
) {
}
