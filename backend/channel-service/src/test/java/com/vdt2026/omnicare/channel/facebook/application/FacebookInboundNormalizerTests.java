package com.vdt2026.omnicare.channel.facebook.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class FacebookInboundNormalizerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final FacebookInboundNormalizer normalizer = new FacebookInboundNormalizer();

    @Test
    void normalizesMessengerFixtureToInboundMessageEvent() throws IOException {
        FacebookInboundEventCommand command = fixture("fixtures/facebook/messenger-message.json");

        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(command, "corr-messenger-1");

        assertThat(event.eventType()).isEqualTo("message-received");
        assertThat(event.correlationId()).isEqualTo("corr-messenger-1");
        assertThat(event.source()).isEqualTo("channel-service.facebook-simulator");
        assertThat(event.payload().channel()).isEqualTo("FACEBOOK");
        assertThat(event.payload().sourceType()).isEqualTo("MESSAGE");
        assertThat(event.payload().providerAccountId()).isEqualTo("local-page-id");
        assertThat(event.payload().externalConversationId()).isEqualTo("facebook:messenger:local-page-id:fb-user-c");
        assertThat(event.payload().externalMessageId()).isEqualTo("mid.local.facebook.messenger.1001");
        assertThat(event.payload().externalIdentityId()).isEqualTo("fb-user-c");
        assertThat(event.payload().customerDisplayName()).isEqualTo("Le Van C");
        assertThat(event.payload().content()).isEqualTo("Shop oi san pham nay con hang khong?");
    }

    @Test
    void normalizesCommentFixtureToRootCommentConversation() throws IOException {
        FacebookInboundEventCommand command = fixture("fixtures/facebook/page-comment.json");

        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(command, "corr-comment-1");

        assertThat(event.payload().channel()).isEqualTo("FACEBOOK");
        assertThat(event.payload().sourceType()).isEqualTo("COMMENT");
        assertThat(event.payload().providerAccountId()).isEqualTo("local-page-id");
        assertThat(event.payload().externalConversationId()).isEqualTo("facebook:comment:local-page-id:post-2026-demo-001:comment-2026-demo-9001");
        assertThat(event.payload().externalMessageId()).isEqualTo("comment-2026-demo-9001");
        assertThat(event.payload().externalIdentityId()).isEqualTo("fb-user-d");
        assertThat(event.payload().customerDisplayName()).isEqualTo("Pham Thi D");
        assertThat(event.payload().content()).isEqualTo("Gia san pham nay la bao nhieu shop?");
    }

    private FacebookInboundEventCommand fixture(String path) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream).as("fixture %s exists", path).isNotNull();
            return objectMapper.readValue(inputStream, FacebookInboundEventCommand.class);
        }
    }
}
