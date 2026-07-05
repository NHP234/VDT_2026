package com.vdt2026.omnicare.channel.facebook.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vdt2026.omnicare.channel.events.application.InboundEventDispatchService;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundNormalizer;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookSignatureValidator;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationService;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FacebookWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    FacebookWebhookVerificationService.class,
    FacebookWebhookSignatureValidator.class,
    FacebookWebhookExceptionHandler.class
})
@TestPropertySource(properties = {
    "app.facebook.verify-token=verify-me",
    "app.facebook.app-secret=app-secret",
    "app.facebook.mode=real"
})
class FacebookWebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FacebookInboundNormalizer normalizer;

    @MockBean
    private InboundEventDispatchService dispatchService;

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

    @Test
    void acceptsSignedWebhookPayload() throws Exception {
        String body = "{\"object\":\"page\",\"entry\":[]}";

        mockMvc.perform(post("/webhooks/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature("app-secret", body))
                .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("accepted"))
            .andExpect(jsonPath("$.processed").value(false));
    }

    @Test
    void rejectsUnsignedWebhookPayloadInRealMode() throws Exception {
        mockMvc.perform(post("/webhooks/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"object\":\"page\",\"entry\":[]}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Facebook webhook signature verification failed"));
    }

    @Test
    void acceptsMessengerMessageAndDispatches() throws Exception {
        String body = """
            {
              "object": "page",
              "entry": [
                {
                  "id": "123456",
                  "time": 1719878400000,
                  "messaging": [
                    {
                      "sender": { "id": "sender-999" },
                      "recipient": { "id": "123456" },
                      "timestamp": 1719878400000,
                      "message": {
                        "mid": "mid.123",
                        "text": "Hello real messenger"
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mockMvc.perform(post("/webhooks/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature("app-secret", body))
                .content(body))
            .andExpect(status().isAccepted());

        verify(dispatchService).dispatch(any());
    }

    @Test
    void ignoresEchoMessengerMessage() throws Exception {
        String body = """
            {
              "object": "page",
              "entry": [
                {
                  "id": "123456",
                  "messaging": [
                    {
                      "sender": { "id": "123456" },
                      "recipient": { "id": "sender-999" },
                      "message": {
                        "mid": "mid.echo",
                        "text": "Hello echo",
                        "is_echo": true
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mockMvc.perform(post("/webhooks/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature("app-secret", body))
                .content(body))
            .andExpect(status().isAccepted());

        verify(dispatchService, never()).dispatch(any());
    }

    @Test
    void acceptsPageCommentAndDispatches() throws Exception {
        String body = """
            {
              "object": "page",
              "entry": [
                {
                  "id": "123456",
                  "changes": [
                    {
                      "field": "feed",
                      "value": {
                        "item": "comment",
                        "verb": "add",
                        "comment_id": "comment-789",
                        "parent_id": "post-456",
                        "post_id": "post-456",
                        "from": { "id": "commenter-888", "name": "Alice" },
                        "message": "Nice comment!",
                        "created_time": 1719878400
                      }
                    }
                  ]
                }
              ]
            }
            """;

        mockMvc.perform(post("/webhooks/facebook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature("app-secret", body))
                .content(body))
            .andExpect(status().isAccepted());

        verify(dispatchService).dispatch(any());
    }

    private static String signature(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
