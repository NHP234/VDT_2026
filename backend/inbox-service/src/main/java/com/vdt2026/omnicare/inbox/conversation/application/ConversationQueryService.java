package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageRepository;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerEntity;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import jakarta.persistence.criteria.JoinType;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationActivityRepository activityRepository;

    public ConversationQueryService(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public PageView<ConversationListItem> list(
        AuthenticatedAgent currentAgent,
        int page,
        int size,
        Channel channel,
        ConversationStatus status,
        String assignee,
        String search
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
            normalizedPage,
            normalizedSize,
            Sort.by(Sort.Direction.DESC, "lastActivityAt")
        );

        Page<ConversationEntity> result = conversationRepository.findAll(
            conversationSpec(currentAgent, channel, status, assignee, search),
            pageRequest
        );

        return new PageView<>(
            result.getContent().stream().map(this::toListItem).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ConversationDetailView detail(UUID conversationId) {
        ConversationEntity conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        return new ConversationDetailView(
            conversation.id(),
            new CustomerSummary(conversation.customer().id(), conversation.customer().displayName()),
            toChannelIdentitySummary(conversation.channelIdentity()),
            conversation.channel(),
            conversation.sourceType(),
            conversation.status(),
            AgentSummary.from(conversation.assignedAgent()),
            conversation.subject(),
            conversation.lastMessagePreview(),
            conversation.lastActivityAt(),
            conversation.createdAt(),
            conversation.updatedAt(),
            messageRepository.findByConversation_IdOrderByOccurredAtAsc(conversation.id()).stream()
                .map(this::toMessageView)
                .toList(),
            activityRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.id()).stream()
                .map(this::toActivityView)
                .toList()
        );
    }

    private Specification<ConversationEntity> conversationSpec(
        AuthenticatedAgent currentAgent,
        Channel channel,
        ConversationStatus status,
        String assignee,
        String search
    ) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (channel != null) {
                predicates.add(builder.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (assignee != null && !assignee.isBlank()) {
                if ("me".equalsIgnoreCase(assignee)) {
                    predicates.add(builder.equal(root.join("assignedAgent", JoinType.LEFT).get("id"), currentAgent.id()));
                }
                else if ("unassigned".equalsIgnoreCase(assignee)) {
                    predicates.add(builder.isNull(root.get("assignedAgent")));
                }
                else {
                    predicates.add(builder.equal(root.join("assignedAgent", JoinType.LEFT).get("id"), UUID.fromString(assignee)));
                }
            }
            if (search != null && !search.isBlank()) {
                String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
                var customer = root.join("customer");
                var channelIdentity = root.join("channelIdentity");
                predicates.add(builder.or(
                    builder.like(builder.lower(customer.get("displayName")), "%" + normalizedSearch + "%"),
                    builder.equal(builder.lower(channelIdentity.get("externalIdentityId")), normalizedSearch)
                ));
            }

            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private ConversationListItem toListItem(ConversationEntity conversation) {
        return new ConversationListItem(
            conversation.id(),
            conversation.customer().displayName(),
            conversation.channel(),
            conversation.sourceType(),
            conversation.channelIdentity().externalIdentityId(),
            conversation.lastMessagePreview(),
            conversation.lastActivityAt(),
            conversation.status(),
            AgentSummary.from(conversation.assignedAgent())
        );
    }

    private ChannelIdentitySummary toChannelIdentitySummary(ChannelIdentityEntity channelIdentity) {
        return new ChannelIdentitySummary(
            channelIdentity.id(),
            channelIdentity.channel(),
            channelIdentity.providerAccountId(),
            channelIdentity.externalIdentityId(),
            channelIdentity.displayName()
        );
    }

    private MessageView toMessageView(MessageEntity message) {
        return new MessageView(
            message.id(),
            message.direction(),
            message.deliveryStatus(),
            message.externalMessageId(),
            message.content(),
            message.occurredAt(),
            message.createdAt()
        );
    }

    private ActivityView toActivityView(ConversationActivityEntity activity) {
        return new ActivityView(
            activity.id(),
            activity.activityType(),
            AgentSummary.from(activity.actorAgent()),
            activity.oldValue(),
            activity.newValue(),
            activity.createdAt()
        );
    }
}
