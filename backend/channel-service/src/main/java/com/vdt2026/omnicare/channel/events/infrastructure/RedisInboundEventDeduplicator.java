package com.vdt2026.omnicare.channel.events.infrastructure;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventDeduplicator;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class RedisInboundEventDeduplicator implements InboundEventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(RedisInboundEventDeduplicator.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    RedisInboundEventDeduplicator(
        StringRedisTemplate redisTemplate,
        @Value("${app.redis.dedup-ttl-seconds:86400}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(Math.max(1L, ttlSeconds));
    }

    @Override
    public boolean accept(EventEnvelope<NormalizedInboundMessagePayload> event) {
        String key = deduplicationKey(event.payload());

        try {
            Boolean inserted = redisTemplate.opsForValue().setIfAbsent(key, event.eventId().toString(), ttl);
            return Boolean.TRUE.equals(inserted);
        }
        catch (DataAccessException ex) {
            log.warn(
                "Redis deduplication failed for inbound event key {}; publishing and relying on durable Inbox idempotency: {}",
                key,
                ex.toString()
            );
            return true;
        }
    }

    static String deduplicationKey(NormalizedInboundMessagePayload payload) {
        String channel = normalize(payload.channel());
        String providerAccountId = normalize(payload.providerAccountId());
        String sourceType = normalize(payload.sourceType());
        String externalMessageId = normalize(payload.externalMessageId());
        return "dedup:inbound:%s:%s:%s:%s".formatted(channel, providerAccountId, sourceType, externalMessageId);
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().toLowerCase();
    }
}
