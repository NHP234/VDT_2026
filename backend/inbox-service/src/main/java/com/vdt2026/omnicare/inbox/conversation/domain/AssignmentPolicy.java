package com.vdt2026.omnicare.inbox.conversation.domain;

import java.util.UUID;

public final class AssignmentPolicy {

    private AssignmentPolicy() {
    }

    public static boolean canAssign(UUID actingAgentId, UUID newAssigneeId) {
        return actingAgentId != null;
    }
}
