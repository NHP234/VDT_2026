package com.vdt2026.omnicare.inbox.conversation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationStatusPolicyTests {

    @Test
    void newConversationStartsOpen() {
        assertThat(ConversationStatusPolicy.initialStatus()).isEqualTo(ConversationStatus.OPEN);
    }

    @Test
    void inboundMessageKeepsOpenConversationOpen() {
        assertThat(ConversationStatusPolicy.afterInboundMessage(ConversationStatus.OPEN))
            .isEqualTo(ConversationStatus.OPEN);
    }

    @Test
    void inboundMessageReopensPendingConversation() {
        assertThat(ConversationStatusPolicy.afterInboundMessage(ConversationStatus.PENDING))
            .isEqualTo(ConversationStatus.OPEN);
    }

    @Test
    void inboundMessageReopensResolvedConversation() {
        assertThat(ConversationStatusPolicy.afterInboundMessage(ConversationStatus.RESOLVED))
            .isEqualTo(ConversationStatus.OPEN);
    }
}
