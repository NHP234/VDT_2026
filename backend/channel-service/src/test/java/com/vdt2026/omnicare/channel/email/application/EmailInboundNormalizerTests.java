package com.vdt2026.omnicare.channel.email.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailInboundNormalizerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final EmailInboundNormalizer normalizer = new EmailInboundNormalizer();

    @Test
    void normalizesEmailFixtureToInboundMessageEvent() throws IOException {
        EmailInboundEventCommand command = fixture("fixtures/email/inbound-message.json");

        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(command, "corr-email-1");

        assertThat(event.eventType()).isEqualTo("message-received");
        assertThat(event.correlationId()).isEqualTo("corr-email-1");
        assertThat(event.source()).isEqualTo("channel-service.email-simulator");
        assertThat(event.payload().channel()).isEqualTo("EMAIL");
        assertThat(event.payload().sourceType()).isEqualTo("EMAIL");
        assertThat(event.payload().providerAccountId()).isEqualTo("demo@example.test");
        assertThat(event.payload().externalConversationId())
            .isEqualTo("email:demo@example.test:<email-demo-1001@example.test>");
        assertThat(event.payload().externalMessageId()).isEqualTo("<email-demo-1001@example.test>");
        assertThat(event.payload().externalIdentityId()).isEqualTo("tran.b@example.test");
        assertThat(event.payload().customerDisplayName()).isEqualTo("Tran Thi B");
        assertThat(event.payload().subject()).isEqualTo("Can ho tro don hang #42");
        assertThat(event.payload().content()).isEqualTo("Minh can ho tro ve don hang #42");
    }

    @Test
    void threadsReplyByReferencesBeforeInReplyTo() {
        EmailInboundEventCommand command = new EmailInboundEventCommand(
            "DEMO@EXAMPLE.TEST",
            "Customer@Example.Test",
            "",
            "demo@example.test",
            "<email-demo-1003@example.test>",
            "Re: Can ho tro don hang #42",
            "Minh bo sung thong tin don hang.",
            Instant.parse("2026-06-29T05:15:00Z"),
            "<email-demo-1002@example.test>",
            List.of("<email-demo-1001@example.test>", "<email-demo-1002@example.test>")
        );

        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(command, "corr-email-thread");

        assertThat(event.payload().providerAccountId()).isEqualTo("demo@example.test");
        assertThat(event.payload().externalIdentityId()).isEqualTo("customer@example.test");
        assertThat(event.payload().customerDisplayName()).isEqualTo("customer@example.test");
        assertThat(event.payload().externalConversationId())
            .isEqualTo("email:demo@example.test:<email-demo-1001@example.test>");
    }

    @Test
    void truncatesLongTextBeforePublishingToInbox() {
        String longContent = "a".repeat(10_050);
        EmailInboundEventCommand command = new EmailInboundEventCommand(
            "demo@example.test",
            "customer@example.test",
            "Customer",
            "demo@example.test",
            "<long@example.test>",
            "Long email",
            longContent,
            Instant.parse("2026-06-29T05:15:00Z"),
            null,
            List.of()
        );

        EventEnvelope<NormalizedInboundMessagePayload> event = normalizer.normalize(command, "corr-long-email");

        assertThat(event.payload().content()).hasSize(10_000);
    }

    private EmailInboundEventCommand fixture(String path) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(inputStream).as("fixture %s exists", path).isNotNull();
            return objectMapper.readValue(inputStream, EmailInboundEventCommand.class);
        }
    }
}
