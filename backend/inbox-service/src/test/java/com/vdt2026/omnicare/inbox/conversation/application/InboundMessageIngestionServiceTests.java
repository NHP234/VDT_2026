package com.vdt2026.omnicare.inbox.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vdt2026.omnicare.inbox.conversation.application.InboundMessageIngestionService.IngestionResult;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
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
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventEntity;
import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.ProcessedEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InboundMessageIngestionServiceTests {

    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final ChannelIdentityRepository channelIdentityRepository = mock(ChannelIdentityRepository.class);
    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final ConversationActivityRepository activityRepository = mock(ConversationActivityRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T02:30:00Z"), ZoneOffset.UTC);

    private final InboundMessageIngestionService service = new InboundMessageIngestionService(
        processedEventRepository,
        customerRepository,
        channelIdentityRepository,
        conversationRepository,
        messageRepository,
        activityRepository,
        clock
    );

    @BeforeEach
    void setUp() {
        when(customerRepository.save(any(CustomerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(channelIdentityRepository.save(any(ChannelIdentityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepository.save(any(ConversationActivityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processedEventRepository.save(any(ProcessedEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsCustomerIdentityConversationMessageAndActivityForNewInboundMessage() {
        InboundMessageReceivedEvent event = event("70000000-0000-0000-0000-000000000001", "fb-user-c", "mid.local.facebook.1001");
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findByChannelAndProviderAccountIdAndExternalMessageId(Channel.FACEBOOK, "local-page-id", "mid.local.facebook.1001"))
            .thenReturn(Optional.empty());
        when(channelIdentityRepository.findByChannelAndProviderAccountIdAndExternalIdentityId(Channel.FACEBOOK, "local-page-id", "fb-user-c"))
            .thenReturn(Optional.empty());
        when(conversationRepository.findByChannelAndProviderAccountIdAndSourceTypeAndExternalConversationId(
            Channel.FACEBOOK,
            "local-page-id",
            ConversationSourceType.MESSAGE,
            "facebook:messenger:local-page-id:fb-user-c"
        )).thenReturn(Optional.empty());

        IngestionResult result = service.ingest(event);

        assertThat(result).isEqualTo(IngestionResult.CREATED);
        ArgumentCaptor<ConversationEntity> conversationCaptor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        assertThat(conversationCaptor.getValue().status()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversationCaptor.getValue().lastMessagePreview()).isEqualTo("Shop oi san pham nay con hang khong?");

        ArgumentCaptor<MessageEntity> messageCaptor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().direction()).isEqualTo(MessageDirection.INBOUND);
        assertThat(messageCaptor.getValue().deliveryStatus()).isEqualTo(DeliveryStatus.RECEIVED);
        assertThat(messageCaptor.getValue().externalMessageId()).isEqualTo("mid.local.facebook.1001");

        ArgumentCaptor<ConversationActivityEntity> activityCaptor = ArgumentCaptor.forClass(ConversationActivityEntity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().activityType()).isEqualTo(ConversationActivityType.MESSAGE_RECEIVED);
        assertThat(activityCaptor.getValue().newValue()).isEqualTo("mid.local.facebook.1001");
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void reopensExistingConversationWhenInboundMessageArrives() {
        InboundMessageReceivedEvent event = event("70000000-0000-0000-0000-000000000002", "fb-user-c", "mid.local.facebook.1002");
        CustomerEntity customer = new CustomerEntity(UUID.randomUUID(), "Le Van C", Instant.parse("2026-06-29T00:00:00Z"));
        ChannelIdentityEntity identity = new ChannelIdentityEntity(
            UUID.randomUUID(),
            customer,
            Channel.FACEBOOK,
            "local-page-id",
            "fb-user-c",
            "Le Van C",
            Instant.parse("2026-06-29T00:00:00Z")
        );
        ConversationEntity conversation = new ConversationEntity(
            UUID.randomUUID(),
            customer,
            identity,
            Channel.FACEBOOK,
            ConversationSourceType.MESSAGE,
            ConversationStatus.RESOLVED,
            null,
            "local-page-id",
            "facebook:messenger:local-page-id:fb-user-c",
            null,
            "Old preview",
            Instant.parse("2026-06-29T00:00:00Z"),
            Instant.parse("2026-06-29T00:00:00Z"),
            Instant.parse("2026-06-29T00:00:00Z")
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findByChannelAndProviderAccountIdAndExternalMessageId(Channel.FACEBOOK, "local-page-id", "mid.local.facebook.1002"))
            .thenReturn(Optional.empty());
        when(channelIdentityRepository.findByChannelAndProviderAccountIdAndExternalIdentityId(Channel.FACEBOOK, "local-page-id", "fb-user-c"))
            .thenReturn(Optional.of(identity));
        when(conversationRepository.findByChannelAndProviderAccountIdAndSourceTypeAndExternalConversationId(
            Channel.FACEBOOK,
            "local-page-id",
            ConversationSourceType.MESSAGE,
            "facebook:messenger:local-page-id:fb-user-c"
        )).thenReturn(Optional.of(conversation));

        IngestionResult result = service.ingest(event);

        assertThat(result).isEqualTo(IngestionResult.CREATED);
        assertThat(conversation.status()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.lastMessagePreview()).isEqualTo("Shop oi san pham nay con hang khong?");
        verify(conversationRepository, never()).save(any(ConversationEntity.class));
        verify(customerRepository, never()).save(any(CustomerEntity.class));
    }

    @Test
    void ignoresAlreadyProcessedEvent() {
        InboundMessageReceivedEvent event = event("70000000-0000-0000-0000-000000000003", "fb-user-c", "mid.local.facebook.1003");
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.of(new ProcessedEventEntity(
            UUID.randomUUID(),
            event.eventId().toString(),
            event.eventType(),
            event.source(),
            event.correlationId(),
            Instant.parse("2026-06-30T02:30:00Z")
        )));

        IngestionResult result = service.ingest(event);

        assertThat(result).isEqualTo(IngestionResult.DUPLICATE_EVENT);
        verifyNoInteractions(customerRepository, channelIdentityRepository, conversationRepository, messageRepository, activityRepository);
    }

    @Test
    void recordsEnvelopeWhenExternalMessageWasAlreadyStored() {
        InboundMessageReceivedEvent event = event("70000000-0000-0000-0000-000000000004", "fb-user-c", "mid.local.facebook.1004");
        MessageEntity storedMessage = new MessageEntity(
            UUID.randomUUID(),
            mock(ConversationEntity.class),
            Channel.FACEBOOK,
            MessageDirection.INBOUND,
            DeliveryStatus.RECEIVED,
            "local-page-id",
            "mid.local.facebook.1004",
            "Stored message",
            Instant.parse("2026-06-29T00:00:00Z"),
            Instant.parse("2026-06-29T00:00:00Z")
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findByChannelAndProviderAccountIdAndExternalMessageId(Channel.FACEBOOK, "local-page-id", "mid.local.facebook.1004"))
            .thenReturn(Optional.of(storedMessage));

        IngestionResult result = service.ingest(event);

        assertThat(result).isEqualTo(IngestionResult.DUPLICATE_MESSAGE);
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
        verify(messageRepository, never()).save(any(MessageEntity.class));
        verifyNoInteractions(customerRepository, channelIdentityRepository, conversationRepository, activityRepository);
    }

    private InboundMessageReceivedEvent event(String eventId, String externalIdentityId, String externalMessageId) {
        return new InboundMessageReceivedEvent(
            UUID.fromString(eventId),
            "message-received",
            Instant.parse("2026-06-30T02:25:00Z"),
            "corr-test-1",
            "channel-service.facebook-simulator",
            new InboundMessageReceivedPayload(
                Channel.FACEBOOK,
                ConversationSourceType.MESSAGE,
                "local-page-id",
                "facebook:messenger:local-page-id:" + externalIdentityId,
                externalMessageId,
                externalIdentityId,
                "Le Van C",
                " Shop oi san pham nay con hang khong? ",
                Instant.parse("2026-06-30T02:25:00Z")
            )
        );
    }
}
