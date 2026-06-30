package com.vdt2026.omnicare.channel.delivery.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyDeliveryException;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplyResult;
import com.vdt2026.omnicare.channel.delivery.application.OutboundReplySender;
import com.vdt2026.omnicare.channel.delivery.application.ReplyRequestPayload;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "app.facebook.mode", havingValue = "real")
class FacebookGraphApiReplySender implements OutboundReplySender {

    private static final String MESSENGER_SOURCE_TYPE = "MESSAGE";
    private static final String COMMENT_SOURCE_TYPE = "COMMENT";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String graphApiBaseUrl;
    private final String pageAccessToken;

    FacebookGraphApiReplySender(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${app.facebook.graph-api-base-url:https://graph.facebook.com/v20.0}") String graphApiBaseUrl,
        @Value("${app.facebook.page-access-token:}") String pageAccessToken
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.graphApiBaseUrl = trimTrailingSlash(graphApiBaseUrl);
        this.pageAccessToken = pageAccessToken;
    }

    @Override
    public OutboundReplyResult send(ReplyRequestPayload payload) {
        requireText(pageAccessToken, "Facebook page access token is required");

        try {
            ResponseEntity<String> response = switch (payload.sourceType()) {
                case MESSENGER_SOURCE_TYPE -> sendMessengerReply(payload);
                case COMMENT_SOURCE_TYPE -> sendCommentReply(payload);
                default -> throw new OutboundReplyDeliveryException("Unsupported Facebook reply source type " + payload.sourceType());
            };
            return new OutboundReplyResult(providerMessageId(response.getBody()));
        }
        catch (HttpStatusCodeException ex) {
            throw new OutboundReplyDeliveryException(providerFailureReason(ex), null, ex);
        }
    }

    private ResponseEntity<String> sendMessengerReply(ReplyRequestPayload payload) {
        String recipientId = externalIdentityFromMessengerConversation(payload.externalConversationId());
        Map<String, Object> body = Map.of(
            "recipient", Map.of("id", recipientId),
            "messaging_type", "RESPONSE",
            "message", Map.of("text", payload.content())
        );
        return exchange("/%s/messages".formatted(payload.providerAccountId()), body);
    }

    private ResponseEntity<String> sendCommentReply(ReplyRequestPayload payload) {
        String commentId = rootCommentIdFromCommentConversation(payload.externalConversationId());
        Map<String, Object> body = Map.of("message", payload.content());
        return exchange("/%s/comments".formatted(commentId), body);
    }

    private ResponseEntity<String> exchange(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(pageAccessToken);
        return restTemplate.exchange(graphApiBaseUrl + path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String providerMessageId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String messageId = root.path("message_id").asText(null);
            if (StringUtils.hasText(messageId)) {
                return messageId;
            }
            String id = root.path("id").asText(null);
            if (StringUtils.hasText(id)) {
                return id;
            }
        }
        catch (Exception ex) {
            throw new OutboundReplyDeliveryException("Facebook reply response could not be parsed", null, ex);
        }
        throw new OutboundReplyDeliveryException("Facebook reply response did not include a provider message ID");
    }

    private String providerFailureReason(HttpStatusCodeException ex) {
        try {
            JsonNode error = objectMapper.readTree(ex.getResponseBodyAsString()).path("error");
            String message = error.path("message").asText(null);
            if (StringUtils.hasText(message)) {
                return "Facebook delivery failed: " + message;
            }
        }
        catch (Exception ignored) {
            // Fall through to a stable generic reason.
        }
        return "Facebook delivery failed with HTTP " + ex.getStatusCode().value();
    }

    static String externalIdentityFromMessengerConversation(String externalConversationId) {
        String[] parts = externalConversationId.split(":");
        if (parts.length < 4 || !"facebook".equals(parts[0]) || !"messenger".equals(parts[1])) {
            throw new OutboundReplyDeliveryException("Invalid Messenger conversation ID");
        }
        return parts[3];
    }

    static String rootCommentIdFromCommentConversation(String externalConversationId) {
        String[] parts = externalConversationId.split(":");
        if (parts.length < 5 || !"facebook".equals(parts[0]) || !"comment".equals(parts[1])) {
            throw new OutboundReplyDeliveryException("Invalid comment conversation ID");
        }
        return parts[4];
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new OutboundReplyDeliveryException(message);
        }
    }
}
