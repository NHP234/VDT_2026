package com.vdt2026.omnicare.channel.facebook.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacebookWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({FacebookWebhookVerificationService.class, FacebookWebhookExceptionHandler.class})
@TestPropertySource(properties = "app.facebook.verify-token=verify-me")
class FacebookWebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPlainTextChallengeWhenVerificationSucceeds() throws Exception {
        mockMvc.perform(get("/webhooks/facebook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "verify-me")
                .param("hub.challenge", "challenge-42"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string("challenge-42"));
    }

    @Test
    void rejectsWrongVerifyToken() throws Exception {
        mockMvc.perform(get("/webhooks/facebook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "wrong-token")
                .param("hub.challenge", "challenge-42"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Facebook webhook verification failed"));
    }

    @Test
    void rejectsMissingRequiredParameter() throws Exception {
        mockMvc.perform(get("/webhooks/facebook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "verify-me"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Missing request parameter"));
    }
}
