package com.vdt2026.omnicare.channel.simulator.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundNormalizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacebookSimulatorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({FacebookInboundNormalizer.class, SimulatorExceptionHandler.class})
@TestPropertySource(properties = "app.facebook.mode=simulator")
class FacebookSimulatorControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
            .andExpect(jsonPath("$.published").value(false))
            .andExpect(jsonPath("$.event.correlationId").value("corr-http-1"))
            .andExpect(jsonPath("$.event.payload.sourceType").value("MESSAGE"))
            .andExpect(jsonPath("$.event.payload.externalConversationId").value("facebook:messenger:local-page-id:fb-user-c"));
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
}
