package com.vdt2026.omnicare.channel.delivery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyDeliveryException;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyResult;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestPayload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class FacebookGraphApiReplySenderTests {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    private final FacebookGraphApiReplySender sender = new FacebookGraphApiReplySender(
        restTemplate,
        JsonMapper.builder().findAndAddModules().build(),
        "https://graph.facebook.test/v20.0",
        "page-token"
    );

    @Test
    void sendsMessengerReplyUsingGraphSendApiContract() {
        server.expect(once(), requestTo("https://graph.facebook.test/v20.0/local-page-id/messages"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer page-token"))
            .andExpect(jsonPath("$.recipient.id").value("fb-user-a"))
            .andExpect(jsonPath("$.messaging_type").value("RESPONSE"))
            .andExpect(jsonPath("$.message.text").value("Hello Messenger customer"))
            .andRespond(withSuccess("{\"recipient_id\":\"fb-user-a\",\"message_id\":\"mid.provider.1\"}", MediaType.APPLICATION_JSON));

        OutboundReplyResult result = sender.send(messengerPayload());

        assertThat(result.providerMessageId()).isEqualTo("mid.provider.1");
        server.verify();
    }

    @Test
    void sendsCommentReplyUsingGraphCommentsContract() {
        server.expect(once(), requestTo("https://graph.facebook.test/v20.0/comment-root-1/comments"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer page-token"))
            .andExpect(jsonPath("$.message").value("Hello comment customer"))
            .andRespond(withSuccess("{\"id\":\"comment-reply-1\"}", MediaType.APPLICATION_JSON));

        OutboundReplyResult result = sender.send(commentPayload());

        assertThat(result.providerMessageId()).isEqualTo("comment-reply-1");
        server.verify();
    }

    @Test
    void mapsGraphApiErrorToDeliveryException() {
        server.expect(once(), requestTo("https://graph.facebook.test/v20.0/local-page-id/messages"))
            .andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"Invalid OAuth access token.\"}}"));

        assertThatThrownBy(() -> sender.send(messengerPayload()))
            .isInstanceOf(OutboundReplyDeliveryException.class)
            .hasMessage("Facebook delivery failed: Invalid OAuth access token.");
    }

    @Test
    void extractsMessengerRecipientFromNormalizedConversationId() {
        assertThat(FacebookGraphApiReplySender.externalIdentityFromMessengerConversation(
            "facebook:messenger:local-page-id:fb-user-a"
        )).isEqualTo("fb-user-a");
    }

    @Test
    void extractsRootCommentIdFromNormalizedConversationId() {
        assertThat(FacebookGraphApiReplySender.rootCommentIdFromCommentConversation(
            "facebook:comment:local-page-id:post-1:comment-root-1"
        )).isEqualTo("comment-root-1");
    }

    private ReplyRequestPayload messengerPayload() {
        return new ReplyRequestPayload(
            UUID.fromString("50000000-0000-0000-0000-000000000001"),
            UUID.fromString("40000000-0000-0000-0000-000000000001"),
            "FACEBOOK",
            "MESSAGE",
            "local-page-id",
            "facebook:messenger:local-page-id:fb-user-a",
            "Hello Messenger customer"
        );
    }

    private ReplyRequestPayload commentPayload() {
        return new ReplyRequestPayload(
            UUID.fromString("50000000-0000-0000-0000-000000000002"),
            UUID.fromString("40000000-0000-0000-0000-000000000002"),
            "FACEBOOK",
            "COMMENT",
            "local-page-id",
            "facebook:comment:local-page-id:post-1:comment-root-1",
            "Hello comment customer"
        );
    }
}
