package com.vdt2026.omnicare.inbox.conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentPolicyTests {

    @Test
    void authenticatedAgentCanAssignToAnotherAgent() {
        assertThat(AssignmentPolicy.canAssign(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    }

    @Test
    void authenticatedAgentCanUnassignConversation() {
        assertThat(AssignmentPolicy.canAssign(UUID.randomUUID(), null)).isTrue();
    }

    @Test
    void anonymousActorCannotAssign() {
        assertThat(AssignmentPolicy.canAssign(null, UUID.randomUUID())).isFalse();
    }
}
