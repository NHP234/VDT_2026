package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    List<MessageEntity> findByConversation_IdOrderByOccurredAtAsc(UUID conversationId);

    Optional<MessageEntity> findByChannelAndProviderAccountIdAndExternalMessageId(
        Channel channel,
        String providerAccountId,
        String externalMessageId
    );
}
