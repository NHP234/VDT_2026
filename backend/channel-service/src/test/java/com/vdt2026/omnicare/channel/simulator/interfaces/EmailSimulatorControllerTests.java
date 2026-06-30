package com.vdt2026.omnicare.channel.simulator.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vdt2026.omnicare.channel.email.application.EmailInboundNormalizer;
import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventDeduplicator;
import com.vdt2026.omnicare.channel.events.application.InboundEventDispatchService;
import com.vdt2026.omnicare.channel.events.application.InboundEventPublisher;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmailSimulatorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    EmailInboundNormalizer.class,
    InboundEventDispatchService.class,
    SimulatorExceptionHandler.class,
    EmailSimulatorControllerTests.PublisherTestConfig.class
})
@TestPropertySource(properties = {
    "app.email.mode=simulator",
    "app.email.mailbox=demo@example.test"
})
class EmailSimulatorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingInboundEventPublisher publisher;

    @Autowired
    private RecordingInboundEventDeduplicator deduplicator;

    @BeforeEach
    void resetRecorders() {
        publisher.clear();
        deduplicator.clear();
    }

    @Test
    void returnsNormalizedEmailEvent() throws Exception {
        String request = """
            {
              "fromEmail": "tran.b@example.test",
              "fromDisplayName": "Tran Thi B",
              "toEmail": "demo@example.test",
              "messageId": "<email-demo-1001@example.test>",
              "subject": "Can ho tro don hang #42",
              "textContent": "Minh can ho tro ve don hang #42",
              "occurredAt": "2026-06-29T04:15:00Z"
            }
            """;

        mockMvc.perform(post("/simulators/email/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", "corr-email-http-1")
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topic").value("inbox.message-received.v1"))
            .andExpect(jsonPath("$.published").value(true))
            .andExpect(jsonPath("$.event.correlationId").value("corr-email-http-1"))
            .andExpect(jsonPath("$.event.payload.channel").value("EMAIL"))
            .andExpect(jsonPath("$.event.payload.sourceType").value("EMAIL"))
            .andExpect(jsonPath("$.event.payload.subject").value("Can ho tro don hang #42"))
            .andExpect(jsonPath("$.event.payload.externalConversationId")
                .value("email:demo@example.test:<email-demo-1001@example.test>"));

        assertThat(publisher.events()).hasSize(1);
        assertThat(publisher.events().getFirst().payload().externalMessageId()).isEqualTo("<email-demo-1001@example.test>");
    }

    @Test
    void skipsPublishingDuplicateEmailEvent() throws Exception {
        deduplicator.rejectExternalMessageId("<email-demo-duplicate@example.test>");

        String request = """
            {
              "fromEmail": "tran.b@example.test",
              "toEmail": "demo@example.test",
              "messageId": "<email-demo-duplicate@example.test>",
              "subject": "Duplicate",
              "textContent": "Duplicate email body",
              "occurredAt": "2026-06-29T04:15:00Z"
            }
            """;

        mockMvc.perform(post("/simulators/email/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", "corr-email-duplicate")
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.published").value(false))
            .andExpect(jsonPath("$.event.payload.externalMessageId").value("<email-demo-duplicate@example.test>"));

        assertThat(publisher.events()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredEmailFields() throws Exception {
        String request = """
            {
              "fromEmail": "tran.b@example.test",
              "textContent": "Missing message id"
            }
            """;

        mockMvc.perform(post("/simulators/email/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid simulator event"))
            .andExpect(jsonPath("$.detail").value("Email messageId is required"));
    }

    @TestConfiguration
    static class PublisherTestConfig {

        @Bean
        RecordingInboundEventPublisher recordingInboundEventPublisher() {
            return new RecordingInboundEventPublisher();
        }

        @Bean
        RecordingInboundEventDeduplicator recordingInboundEventDeduplicator() {
            return new RecordingInboundEventDeduplicator();
        }
    }

    static class RecordingInboundEventPublisher implements InboundEventPublisher {

        private final List<EventEnvelope<NormalizedInboundMessagePayload>> events = new ArrayList<>();

        @Override
        public void publish(EventEnvelope<NormalizedInboundMessagePayload> event) {
            events.add(event);
        }

        List<EventEnvelope<NormalizedInboundMessagePayload>> events() {
            return events;
        }

        void clear() {
            events.clear();
        }
    }

    static class RecordingInboundEventDeduplicator implements InboundEventDeduplicator {

        private final List<String> rejectedExternalMessageIds = new ArrayList<>();

        @Override
        public boolean accept(EventEnvelope<NormalizedInboundMessagePayload> event) {
            return !rejectedExternalMessageIds.contains(event.payload().externalMessageId());
        }

        void rejectExternalMessageId(String externalMessageId) {
            rejectedExternalMessageIds.add(externalMessageId);
        }

        void clear() {
            rejectedExternalMessageIds.clear();
        }
    }
}
