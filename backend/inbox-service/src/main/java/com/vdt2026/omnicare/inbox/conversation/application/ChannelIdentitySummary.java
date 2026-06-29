package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import java.util.UUID;

public record ChannelIdentitySummary(
    UUID id,
    Channel channel,
    String providerAccountId,
    String externalIdentityId,
    String displayName
) {
}
