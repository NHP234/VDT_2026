package com.vdt2026.omnicare.channel.events.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisInboundEventDeduplicatorTests {

    @Test
    void acceptsNewEventAndStoresDeduplicationKeyWithTtl() {
        RedisFixture fixture = redisFixture();
        EventEnvelope<NormalizedInboundMessagePayload> event = event("mid-local-1");
        String key = "dedup:inbound:facebook:local-page-id:message:mid-local-1";
        when(fixture.valueOperations().setIfAbsent(eq(key), eq(event.eventId().toString()), eq(Duration.ofSeconds(60))))
            .thenReturn(true);

        RedisInboundEventDeduplicator deduplicator = new RedisInboundEventDeduplicator(fixture.redisTemplate(), 60);

        assertThat(deduplicator.accept(event)).isTrue();
        verify(fixture.valueOperations()).setIfAbsent(key, event.eventId().toString(), Duration.ofSeconds(60));
    }

    @Test
    void rejectsDuplicateEventWhenKeyAlreadyExists() {
        RedisFixture fixture = redisFixture();
        EventEnvelope<NormalizedInboundMessagePayload> event = event("mid-local-duplicate");
        String key = "dedup:inbound:facebook:local-page-id:message:mid-local-duplicate";
        when(fixture.valueOperations().setIfAbsent(eq(key), eq(event.eventId().toString()), eq(Duration.ofSeconds(60))))
            .thenReturn(false);

        RedisInboundEventDeduplicator deduplicator = new RedisInboundEventDeduplicator(fixture.redisTemplate(), 60);

        assertThat(deduplicator.accept(event)).isFalse();
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        RedisFixture fixture = redisFixture();
        EventEnvelope<NormalizedInboundMessagePayload> event = event("mid-local-timeout");
        String key = "dedup:inbound:facebook:local-page-id:message:mid-local-timeout";
        when(fixture.valueOperations().setIfAbsent(eq(key), eq(event.eventId().toString()), eq(Duration.ofSeconds(60))))
            .thenThrow(new QueryTimeoutException("redis timeout"));

        RedisInboundEventDeduplicator deduplicator = new RedisInboundEventDeduplicator(fixture.redisTemplate(), 60);

        assertThat(deduplicator.accept(event)).isTrue();
    }

    @Test
    void buildsStableLowercaseDeduplicationKey() {
        NormalizedInboundMessagePayload payload = new NormalizedInboundMessagePayload(
            "FACEBOOK",
            "COMMENT",
            "Local-Page-Id",
            "facebook:comment:local-page-id:post-1:comment-1",
            "Comment-1",
            "fb-user",
            "Facebook User",
            null,
            "hello",
            Instant.parse("2026-06-30T00:00:00Z")
        );

        assertThat(RedisInboundEventDeduplicator.deduplicationKey(payload))
            .isEqualTo("dedup:inbound:facebook:local-page-id:comment:comment-1");
    }

    @SuppressWarnings("unchecked")
    private RedisFixture redisFixture() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        return new RedisFixture(redisTemplate, valueOperations);
    }

    private EventEnvelope<NormalizedInboundMessagePayload> event(String externalMessageId) {
        Instant occurredAt = Instant.parse("2026-06-30T00:00:00Z");
        return new EventEnvelope<>(
            UUID.randomUUID(),
            "message-received",
            occurredAt,
            "corr-test",
            "channel-service.facebook-simulator",
            new NormalizedInboundMessagePayload(
                "FACEBOOK",
                "MESSAGE",
                "local-page-id",
                "facebook:messenger:local-page-id:fb-user",
                externalMessageId,
                "fb-user",
                "Facebook User",
                null,
                "hello",
                occurredAt
            )
        );
    }

    private record RedisFixture(
        StringRedisTemplate redisTemplate,
        ValueOperations<String, String> valueOperations
    ) {
    }
}
