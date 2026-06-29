package com.vdt2026.omnicare.inbox.conversation.interfaces;

import com.vdt2026.omnicare.inbox.conversation.application.ConversationDetailView;
import com.vdt2026.omnicare.inbox.conversation.application.ConversationListItem;
import com.vdt2026.omnicare.inbox.conversation.application.ConversationCommandService;
import com.vdt2026.omnicare.inbox.conversation.application.ConversationQueryService;
import com.vdt2026.omnicare.inbox.conversation.application.PageView;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
class ConversationController {

    private final ConversationQueryService conversationQueryService;
    private final ConversationCommandService conversationCommandService;

    ConversationController(ConversationQueryService conversationQueryService, ConversationCommandService conversationCommandService) {
        this.conversationQueryService = conversationQueryService;
        this.conversationCommandService = conversationCommandService;
    }

    @GetMapping
    PageView<ConversationListItem> listConversations(
        @AuthenticationPrincipal AuthenticatedAgent currentAgent,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Channel channel,
        @RequestParam(required = false) ConversationStatus status,
        @RequestParam(required = false) String assignee,
        @RequestParam(required = false) String search
    ) {
        return conversationQueryService.list(currentAgent, page, size, channel, status, assignee, search);
    }

    @GetMapping("/{id}")
    ConversationDetailView getConversation(@PathVariable UUID id) {
        return conversationQueryService.detail(id);
    }

    @PatchMapping("/{id}/status")
    ConversationDetailView changeStatus(
        @AuthenticationPrincipal AuthenticatedAgent currentAgent,
        @PathVariable UUID id,
        @Valid @RequestBody ChangeStatusRequest request
    ) {
        conversationCommandService.changeStatus(id, request.status(), currentAgent);
        return conversationQueryService.detail(id);
    }

    @PatchMapping("/{id}/assignee")
    ConversationDetailView changeAssignee(
        @AuthenticationPrincipal AuthenticatedAgent currentAgent,
        @PathVariable UUID id,
        @RequestBody ChangeAssigneeRequest request
    ) {
        conversationCommandService.changeAssignee(id, request.assignedAgentId(), currentAgent);
        return conversationQueryService.detail(id);
    }
}
