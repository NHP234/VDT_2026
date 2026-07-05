package com.vdt2026.omnicare.channel.facebook.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt2026.omnicare.channel.events.application.InboundEventDispatchService;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundEventCommand;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundEventType;
import com.vdt2026.omnicare.channel.facebook.application.FacebookInboundNormalizer;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookSignatureValidator;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/facebook")
class FacebookWebhookController {

    private final FacebookWebhookVerificationService verificationService;
    private final FacebookWebhookSignatureValidator signatureValidator;
    private final FacebookInboundNormalizer normalizer;
    private final InboundEventDispatchService dispatchService;
    private final ObjectMapper objectMapper;

    FacebookWebhookController(
        FacebookWebhookVerificationService verificationService,
        FacebookWebhookSignatureValidator signatureValidator,
        FacebookInboundNormalizer normalizer,
        InboundEventDispatchService dispatchService,
        ObjectMapper objectMapper
    ) {
        this.verificationService = verificationService;
        this.signatureValidator = signatureValidator;
        this.normalizer = normalizer;
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> verify(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String verifyToken,
        @RequestParam("hub.challenge") String challenge
    ) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(verificationService.verify(mode, verifyToken, challenge));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FacebookWebhookReceiveResponse> receive(
        @RequestBody String rawBody,
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader,
        @RequestHeader(name = "X-Correlation-Id", required = false) String correlationId
    ) {
        signatureValidator.validate(rawBody, signatureHeader);

        String effectiveCorrelationId = StringUtils.hasText(correlationId) ? correlationId : UUID.randomUUID().toString();

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String objectType = root.path("object").asText("");
            if ("page".equals(objectType)) {
                JsonNode entries = root.path("entry");
                if (entries.isArray()) {
                    for (JsonNode entry : entries) {
                        String pageId = entry.path("id").asText(null);

                        // Process Messenger messages
                        JsonNode messagings = entry.path("messaging");
                        if (messagings.isArray()) {
                            for (JsonNode messaging : messagings) {
                                JsonNode messageNode = messaging.path("message");
                                if (!messageNode.isMissingNode()) {
                                    boolean isEcho = messageNode.path("is_echo").asBoolean(false);
                                    String text = messageNode.path("text").asText(null);
                                    String messageId = messageNode.path("mid").asText(null);
                                    String senderId = messaging.path("sender").path("id").asText(null);
                                    long timestamp = messaging.path("timestamp").asLong(0);

                                    // Ignore echo messages and messages with no text content
                                    if (!isEcho && StringUtils.hasText(text) && StringUtils.hasText(senderId) && StringUtils.hasText(messageId)) {
                                        Instant occurredAt = timestamp > 0 ? Instant.ofEpochMilli(timestamp) : Instant.now();
                                        FacebookInboundEventCommand command = new FacebookInboundEventCommand(
                                            FacebookInboundEventType.MESSENGER_MESSAGE,
                                            pageId,
                                            senderId,
                                            null,
                                            null,
                                            null,
                                            messageId,
                                            null,
                                            null,
                                            null,
                                            text,
                                            occurredAt
                                        );
                                        dispatchService.dispatch(normalizer.normalize(command, effectiveCorrelationId));
                                    }
                                }
                            }
                        }

                        // Process Page comments
                        JsonNode changes = entry.path("changes");
                        if (changes.isArray()) {
                            for (JsonNode change : changes) {
                                String field = change.path("field").asText("");
                                if ("feed".equals(field)) {
                                    JsonNode valueNode = change.path("value");
                                    String item = valueNode.path("item").asText("");
                                    String verb = valueNode.path("verb").asText("");

                                    if ("comment".equals(item) && ("add".equals(verb) || "edited".equals(verb))) {
                                        String commenterId = valueNode.path("from").path("id").asText(null);
                                        String commenterName = valueNode.path("from").path("name").asText(null);
                                        String commentId = valueNode.path("comment_id").asText(null);
                                        String postId = valueNode.path("post_id").asText(null);
                                        String parentId = valueNode.path("parent_id").asText(null);
                                        String text = valueNode.path("message").asText(null);
                                        long timestamp = valueNode.path("created_time").asLong(0);

                                        // Ignore echo comments (posted by the Page itself)
                                        if (StringUtils.hasText(commenterId) && !commenterId.equals(pageId)
                                                && StringUtils.hasText(text) && StringUtils.hasText(commentId) && StringUtils.hasText(postId)) {

                                            String rootCommentId = null;
                                            if (StringUtils.hasText(parentId) && !parentId.equals(postId)) {
                                                rootCommentId = parentId;
                                            }

                                            Instant occurredAt = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now();
                                            FacebookInboundEventCommand command = new FacebookInboundEventCommand(
                                                FacebookInboundEventType.PAGE_COMMENT,
                                                pageId,
                                                null,
                                                null,
                                                commenterId,
                                                commenterName,
                                                null,
                                                postId,
                                                commentId,
                                                rootCommentId,
                                                text,
                                                occurredAt
                                            );
                                            dispatchService.dispatch(normalizer.normalize(command, effectiveCorrelationId));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log warning, but return accepted to let Facebook know we received the POST
            System.err.println("Error processing Facebook webhook: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.accepted().body(new FacebookWebhookReceiveResponse("accepted", false));
    }
}
