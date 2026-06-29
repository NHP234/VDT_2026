package com.vdt2026.omnicare.inbox.conversation.interfaces;

import java.util.UUID;

record ChangeAssigneeRequest(
    UUID assignedAgentId
) {
}
