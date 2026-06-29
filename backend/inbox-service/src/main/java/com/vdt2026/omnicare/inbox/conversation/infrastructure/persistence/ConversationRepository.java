package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID>, JpaSpecificationExecutor<ConversationEntity> {

    @Override
    @EntityGraph(attributePaths = {"customer", "channelIdentity", "assignedAgent"})
    Optional<ConversationEntity> findById(UUID id);

    Optional<ConversationEntity> findByChannelAndProviderAccountIdAndSourceTypeAndExternalConversationId(
        Channel channel,
        String providerAccountId,
        ConversationSourceType sourceType,
        String externalConversationId
    );
}
