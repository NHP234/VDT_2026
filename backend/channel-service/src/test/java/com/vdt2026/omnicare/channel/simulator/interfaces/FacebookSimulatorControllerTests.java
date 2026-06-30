package com.vdt2026.omnicare.channel.simulator.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.vdt2026.omnicare.channel.events.application.EventEnvelope;
import com.vdt2026.omnicare.channel.events.application.InboundEventPublisher;
import com.vdt2026.omnicare.channel.events.application.NormalizedInboundMessagePayload;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundNormalizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacebookSimulatorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({FacebookInboundNormalizer.class, SimulatorExceptionHandler.class, FacebookSimulatorControllerTests.PublisherTestConfig.class})
@TestPropertySource(properties = "app.facebook.mode=simulator")
class FacebookSimulatorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingInboundEventPublisher publisher;

    @Test
    void returnsNormalizedMessengerEvent() throws Exception {
        String request = """
            {
              "type": "MESSENGER_MESSAGE",
              "pageId": "local-page-id",
              "senderId": "fb-user-c",
              "senderDisplayName": "Le Van C",
              "messageId": "mid.local.facebook.messenger.1001",
              "content": "Shop oi san pham nay con hang khong?",
              "occurredAt": "2026-06-29T02:15:00Z"
            }
            """;

        mockMvc.perform(post("/simulators/facebook/events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", "corr-http-1")
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topic").value("inbox.message-received.v1"))
            .andExpect(jsonPath("$.published").value(true))
            .andExpect(jsonPath("$.event.correlationId").value("corr-http-1"))
            .andExpect(jsonPath("$.event.payload.sourceType").value("MESSAGE"))
            .andExpect(jsonPath("$.event.payload.externalConversationId").value("facebook:messenger:local-page-id:fb-user-c"));

        assertThat(publisher.events()).hasSize(1);
        assertThat(publisher.events().getFirst().payload().externalMessageId()).isEqualTo("mid.local.facebook.messenger.1001");
    }

    @Test
    void rejectsMissingTypeSpecificFields() throws Exception {
        String request = """
            {
              "type": "PAGE_COMMENT",
              "pageId": "local-page-id",
              "commenterId": "fb-user-d",
              "content": "Gia san pham nay la bao nhieu shop?"
            }
            """;

        mockMvc.perform(post("/simulators/facebook/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid simulator event"))
            .andExpect(jsonPath("$.detail").value("Comment postId is required"));
    }

    @TestConfiguration
    static class PublisherTestConfig {

        @Bean
        RecordingInboundEventPublisher recordingInboundEventPublisher() {
            return new RecordingInboundEventPublisher();
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
    }
}
