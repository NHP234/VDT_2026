package com.vdt2026.omnicare.inbox.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vdt2026.omnicare.inbox.conversation.application.DeliveryResultIngestionService.DeliveryResult;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationActivityRepository;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.ConversationEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageEntity;
import com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence.MessageRepository;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerEntity;
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

class DeliveryResultIngestionServiceTests {

    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final ConversationActivityRepository activityRepository = mock(ConversationActivityRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-30T04:00:00Z"), ZoneOffset.UTC);

    private final DeliveryResultIngestionService service = new DeliveryResultIngestionService(
        processedEventRepository,
        messageRepository,
        activityRepository,
        clock
    );

    @BeforeEach
    void setUp() {
        when(processedEventRepository.save(any(ProcessedEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepository.save(any(ConversationActivityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void marksQueuedOutboundMessageAsSent() {
        MessageEntity message = outboundMessage(DeliveryStatus.QUEUED);
        DeliveryResultEvent event = event(
            "70000000-0000-0000-0000-000000000101",
            "reply-delivery-succeeded",
            null
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findById(event.payload().messageId())).thenReturn(Optional.of(message));

        DeliveryResult result = service.ingest(event);

        assertThat(result).isEqualTo(DeliveryResult.UPDATED);
        assertThat(message.deliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(message.externalMessageId()).isEqualTo("simulated:50000000-0000-0000-0000-000000000001");

        ArgumentCaptor<ConversationActivityEntity> activityCaptor = ArgumentCaptor.forClass(ConversationActivityEntity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().activityType()).isEqualTo(ConversationActivityType.DELIVERY_STATUS_CHANGED);
        assertThat(activityCaptor.getValue().oldValue()).isEqualTo("QUEUED");
        assertThat(activityCaptor.getValue().newValue()).isEqualTo("SENT");
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void marksQueuedOutboundMessageAsFailedWithReason() {
        MessageEntity message = outboundMessage(DeliveryStatus.QUEUED);
        DeliveryResultEvent event = event(
            "70000000-0000-0000-0000-000000000102",
            "reply-delivery-failed",
            "Simulated provider delivery failure"
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findById(event.payload().messageId())).thenReturn(Optional.of(message));

        DeliveryResult result = service.ingest(event);

        assertThat(result).isEqualTo(DeliveryResult.UPDATED);
        assertThat(message.deliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        ArgumentCaptor<ConversationActivityEntity> activityCaptor = ArgumentCaptor.forClass(ConversationActivityEntity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().newValue()).isEqualTo("FAILED: Simulated provider delivery failure");
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void ignoresAlreadyProcessedDeliveryResult() {
        DeliveryResultEvent event = event(
            "70000000-0000-0000-0000-000000000103",
            "reply-delivery-succeeded",
            null
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.of(new ProcessedEventEntity(
            UUID.randomUUID(),
            event.eventId().toString(),
            event.eventType(),
            event.source(),
            event.correlationId(),
            Instant.parse("2026-06-30T04:00:00Z")
        )));

        DeliveryResult result = service.ingest(event);

        assertThat(result).isEqualTo(DeliveryResult.DUPLICATE_EVENT);
        verifyNoInteractions(messageRepository, activityRepository);
    }

    @Test
    void recordsProcessedEventWhenStatusAlreadyMatches() {
        MessageEntity message = outboundMessage(DeliveryStatus.SENT);
        DeliveryResultEvent event = event(
            "70000000-0000-0000-0000-000000000104",
            "reply-delivery-succeeded",
            null
        );
        when(processedEventRepository.findByEventId(event.eventId().toString())).thenReturn(Optional.empty());
        when(messageRepository.findById(event.payload().messageId())).thenReturn(Optional.of(message));

        DeliveryResult result = service.ingest(event);

        assertThat(result).isEqualTo(DeliveryResult.NO_STATUS_CHANGE);
        verify(activityRepository, never()).save(any(ConversationActivityEntity.class));
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    private DeliveryResultEvent event(String eventId, String eventType, String failureReason) {
        return new DeliveryResultEvent(
            UUID.fromString(eventId),
            eventType,
            Instant.parse("2026-06-30T03:59:00Z"),
            "corr-delivery-1",
            "channel-service.reply-delivery",
            new DeliveryResultPayload(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                "FACEBOOK",
                "MESSAGE",
                "local-page-id",
                "messenger:fb-user-a",
                "simulated:50000000-0000-0000-0000-000000000001",
                Instant.parse("2026-06-30T03:59:00Z"),
                failureReason
            )
        );
    }

    private MessageEntity outboundMessage(DeliveryStatus status) {
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
            UUID.fromString("40000000-0000-0000-0000-000000000001"),
            customer,
            identity,
            Channel.FACEBOOK,
            ConversationSourceType.MESSAGE,
            ConversationStatus.OPEN,
            null,
            "local-page-id",
            "messenger:fb-user-a",
            null,
            "Reply preview",
            Instant.parse("2026-06-30T03:58:00Z"),
            Instant.parse("2026-06-30T03:58:00Z"),
            Instant.parse("2026-06-30T03:58:00Z")
        );
        return new MessageEntity(
            UUID.fromString("50000000-0000-0000-0000-000000000001"),
            conversation,
            Channel.FACEBOOK,
            MessageDirection.OUTBOUND,
            status,
            "local-page-id",
            null,
            "Hello customer",
            Instant.parse("2026-06-30T03:58:00Z"),
            Instant.parse("2026-06-30T03:58:00Z")
        );
    }
}
