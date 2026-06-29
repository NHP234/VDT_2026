package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.conversation.domain.DeliveryStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.MessageDirection;
import com.vdt2026.omnicare.inbox.shared.domain.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(nullable = false, length = 200)
    private String providerAccountId;

    @Column(length = 500)
    private String externalMessageId;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected MessageEntity() {
    }

    public MessageEntity(
        UUID id,
        ConversationEntity conversation,
        Channel channel,
        MessageDirection direction,
        DeliveryStatus deliveryStatus,
        String providerAccountId,
        String externalMessageId,
        String content,
        Instant occurredAt,
        Instant createdAt
    ) {
        this.id = id;
        this.conversation = conversation;
        this.channel = channel;
        this.direction = direction;
        this.deliveryStatus = deliveryStatus;
        this.providerAccountId = providerAccountId;
        this.externalMessageId = externalMessageId;
        this.content = content;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    void beforePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markDeliveryStatus(DeliveryStatus status) {
        deliveryStatus = status;
    }

    public UUID id() {
        return id;
    }

    public DeliveryStatus deliveryStatus() {
        return deliveryStatus;
    }

    public MessageDirection direction() {
        return direction;
    }

    public String externalMessageId() {
        return externalMessageId;
    }
}
