package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.vdt2026.omnicare.inbox.conversation.application.ReplyCommandService;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
class MessageController {

    private final ReplyCommandService replyCommandService;

    MessageController(ReplyCommandService replyCommandService) {
        this.replyCommandService = replyCommandService;
    }

    @PostMapping("/{id}/retry")
    Map<String, UUID> retryMessage(@AuthenticationPrincipal AuthenticatedAgent currentAgent, @PathVariable UUID id) {
        UUID messageId = replyCommandService.retryFailedMessage(id, currentAgent);
        return Map.of("messageId", messageId);
    }
}
