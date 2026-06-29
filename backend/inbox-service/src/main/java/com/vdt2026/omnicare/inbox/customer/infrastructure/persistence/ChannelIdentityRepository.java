package com.vdt2026.omnicare.inbox.customer.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelIdentityRepository extends JpaRepository<ChannelIdentityEntity, UUID> {

    Optional<ChannelIdentityEntity> findByChannelAndProviderAccountIdAndExternalIdentityId(
        Channel channel,
        String providerAccountId,
        String externalIdentityId
    );
}
