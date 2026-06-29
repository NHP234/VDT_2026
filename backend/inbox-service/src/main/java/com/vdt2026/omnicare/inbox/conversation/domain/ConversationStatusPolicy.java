package com.vdt2026.omnicare.inbox.conversation.domain;

public final class ConversationStatusPolicy {

    private ConversationStatusPolicy() {
    }

    public static ConversationStatus initialStatus() {
        return ConversationStatus.OPEN;
    }

    public static ConversationStatus afterInboundMessage(ConversationStatus currentStatus) {
        return switch (currentStatus) {
            case OPEN -> ConversationStatus.OPEN;
            case PENDING, RESOLVED -> ConversationStatus.OPEN;
        };
    }

    public static boolean canAgentSelect(ConversationStatus status) {
        return status != null;
    }
}
