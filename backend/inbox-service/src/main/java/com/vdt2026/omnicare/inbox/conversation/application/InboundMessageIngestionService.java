package com.vdt2026.omnicare.inbox.conversation.application;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageRepository;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityRepository;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerRepository;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InboundMessageIngestionService {

    private final ProcessedEventRepository processedEventRepository;
    private final CustomerRepository customerRepository;
    private final ChannelIdentityRepository channelIdentityRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationActivityRepository activityRepository;
    private final Clock clock;

    @Autowired
    public InboundMessageIngestionService(
        ProcessedEventRepository processedEventRepository,
        CustomerRepository customerRepository,
        ChannelIdentityRepository channelIdentityRepository,
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository
    ) {
        this(
            processedEventRepository,
            customerRepository,
            channelIdentityRepository,
            conversationRepository,
            messageRepository,
            activityRepository,
            Clock.systemUTC()
        );
    }

    InboundMessageIngestionService(
        ProcessedEventRepository processedEventRepository,
        CustomerRepository customerRepository,
        ChannelIdentityRepository channelIdentityRepository,
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        ConversationActivityRepository activityRepository,
        Clock clock
    ) {
        this.processedEventRepository = processedEventRepository;
        this.customerRepository = customerRepository;
        this.channelIdentityRepository = channelIdentityRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    @Transactional
    public IngestionResult ingest(InboundMessageReceivedEvent event) {
        validate(event);
        String eventId = event.eventId().toString();
        if (processedEventRepository.findByEventId(eventId).isPresent()) {
            return IngestionResult.DUPLICATE_EVENT;
        }

        InboundMessageReceivedPayload payload = event.payload();
        if (messageRepository.findByChannelAndProviderAccountIdAndExternalMessageId(
            payload.channel(),
            payload.providerAccountId(),
            payload.externalMessageId()
        ).isPresent()) {
            markProcessed(event);
            return IngestionResult.DUPLICATE_MESSAGE;
        }

        ChannelIdentityEntity channelIdentity = channelIdentity(payload);
        ConversationEntity conversation = conversation(payload, channelIdentity);
        Instant occurredAt = payload.occurredAt();
        String content = payload.content().trim();

        MessageEntity message = messageRepository.save(new MessageEntity(
            UUID.randomUUID(),
            conversation,
            payload.channel(),
            MessageDirection.INBOUND,
            DeliveryStatus.RECEIVED,
            payload.providerAccountId(),
            payload.externalMessageId(),
            content,
            occurredAt,
            Instant.now(clock)
        ));

        conversation.reopenForInboundMessage();
        conversation.updateLastMessage(preview(content), occurredAt);
        activityRepository.save(new ConversationActivityEntity(
            UUID.randomUUID(),
            conversation,
            null,
            ConversationActivityType.MESSAGE_RECEIVED,
            null,
            message.externalMessageId(),
            occurredAt
        ));
        markProcessed(event);
        return IngestionResult.CREATED;
    }

    private ChannelIdentityEntity channelIdentity(InboundMessageReceivedPayload payload) {
        return channelIdentityRepository.findByChannelAndProviderAccountIdAndExternalIdentityId(
            payload.channel(),
            payload.providerAccountId(),
            payload.externalIdentityId()
        ).orElseGet(() -> createChannelIdentity(payload));
    }

    private ChannelIdentityEntity createChannelIdentity(InboundMessageReceivedPayload payload) {
        Instant now = Instant.now(clock);
        CustomerEntity customer = customerRepository.save(new CustomerEntity(
            UUID.randomUUID(),
            displayName(payload),
            now
        ));
        return channelIdentityRepository.save(new ChannelIdentityEntity(
            UUID.randomUUID(),
            customer,
            payload.channel(),
            payload.providerAccountId(),
            payload.externalIdentityId(),
            displayName(payload),
            now
        ));
    }

    private ConversationEntity conversation(InboundMessageReceivedPayload payload, ChannelIdentityEntity channelIdentity) {
        return conversationRepository.findByChannelAndProviderAccountIdAndSourceTypeAndExternalConversationId(
            payload.channel(),
            payload.providerAccountId(),
            payload.sourceType(),
            payload.externalConversationId()
        ).orElseGet(() -> {
            Instant now = Instant.now(clock);
            return conversationRepository.save(new ConversationEntity(
                UUID.randomUUID(),
                channelIdentity.customer(),
                channelIdentity,
                payload.channel(),
                payload.sourceType(),
                ConversationStatus.OPEN,
                null,
                payload.providerAccountId(),
                payload.externalConversationId(),
                null,
                preview(payload.content().trim()),
                payload.occurredAt(),
                now,
                now
            ));
        });
    }

    private void markProcessed(InboundMessageReceivedEvent event) {
        processedEventRepository.save(new ProcessedEventEntity(
            UUID.randomUUID(),
            event.eventId().toString(),
            event.eventType(),
            event.source(),
            event.correlationId(),
            Instant.now(clock)
        ));
    }

    private void validate(InboundMessageReceivedEvent event) {
        if (event == null || event.eventId() == null || event.payload() == null) {
            throw new IllegalArgumentException("Inbound event envelope is required");
        }
        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("Inbound event timestamp is required");
        }
        if (!"message-received".equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported inbound event type");
        }
        requireText(event.source(), "Inbound event source is required");
        InboundMessageReceivedPayload payload = event.payload();
        if (payload.channel() == null || payload.sourceType() == null || payload.occurredAt() == null) {
            throw new IllegalArgumentException("Inbound event channel, source type and timestamp are required");
        }
        requireText(payload.providerAccountId(), "Provider account ID is required");
        requireText(payload.externalConversationId(), "External conversation ID is required");
        requireText(payload.externalMessageId(), "External message ID is required");
        requireText(payload.externalIdentityId(), "External identity ID is required");
        requireText(payload.content(), "Inbound content is required");
        if (payload.content().trim().length() > 10_000) {
            throw new IllegalArgumentException("Inbound content must be 10000 characters or fewer");
        }
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String displayName(InboundMessageReceivedPayload payload) {
        if (StringUtils.hasText(payload.customerDisplayName())) {
            return limit(payload.customerDisplayName().trim(), 160);
        }
        return limit(payload.externalIdentityId(), 160);
    }

    private String preview(String content) {
        return limit(content, 500);
    }

    private String limit(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public enum IngestionResult {
        CREATED,
        DUPLICATE_EVENT,
        DUPLICATE_MESSAGE
    }
}
