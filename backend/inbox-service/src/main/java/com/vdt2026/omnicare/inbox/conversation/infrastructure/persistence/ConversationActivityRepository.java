package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationActivityRepository extends JpaRepository<ConversationActivityEntity, UUID> {

    List<ConversationActivityEntity> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);
}
