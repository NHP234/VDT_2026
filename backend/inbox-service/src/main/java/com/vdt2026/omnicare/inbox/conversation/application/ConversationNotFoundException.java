package com.vdt2026.omnicare.inbox.conversation.application;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
