package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    Optional<ConversationEntity> findByChannelAndProviderAccountIdAndSourceTypeAndExternalConversationId(
        Channel channel,
        String providerAccountId,
        ConversationSourceType sourceType,
        String externalConversationId
    );
}
