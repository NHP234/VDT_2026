package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationSourceType;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatus;
import com.vdt2026.omnicare.inbox.conversation.domain.ConversationStatusPolicy;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.ChannelIdentityEntity;
import com.vdt2026.omnicare.inbox.customer.infrastructure.persistence.CustomerEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_identity_id", nullable = false)
    private ChannelIdentityEntity channelIdentity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private AgentEntity assignedAgent;

    @Column(nullable = false, length = 200)
    private String providerAccountId;

    @Column(nullable = false, length = 500)
    private String externalConversationId;

    @Column(length = 300)
    private String subject;

    @Column(nullable = false, length = 500)
    private String lastMessagePreview;

    @Column(nullable = false)
    private Instant lastActivityAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(
        UUID id,
        CustomerEntity customer,
        ChannelIdentityEntity channelIdentity,
        Channel channel,
        ConversationSourceType sourceType,
        ConversationStatus status,
        AgentEntity assignedAgent,
        String providerAccountId,
        String externalConversationId,
        String subject,
        String lastMessagePreview,
        Instant lastActivityAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.customer = customer;
        this.channelIdentity = channelIdentity;
        this.channel = channel;
        this.sourceType = sourceType;
        this.status = status;
        this.assignedAgent = assignedAgent;
        this.providerAccountId = providerAccountId;
        this.externalConversationId = externalConversationId;
        this.subject = subject;
        this.lastMessagePreview = lastMessagePreview;
        this.lastActivityAt = lastActivityAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void beforePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = ConversationStatusPolicy.initialStatus();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (lastActivityAt == null) {
            lastActivityAt = now;
        }
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public void changeStatus(ConversationStatus newStatus) {
        if (!ConversationStatusPolicy.canAgentSelect(newStatus)) {
            throw new IllegalArgumentException("Conversation status is required");
        }
        status = newStatus;
    }

    public void reopenForInboundMessage() {
        status = ConversationStatusPolicy.afterInboundMessage(status);
    }

    public void assignTo(AgentEntity agent) {
        assignedAgent = agent;
    }

    public void unassign() {
        assignedAgent = null;
    }

    public void updateLastMessage(String preview, Instant occurredAt) {
        lastMessagePreview = preview;
        lastActivityAt = occurredAt;
    }

    public UUID id() {
        return id;
    }

    public CustomerEntity customer() {
        return customer;
    }

    public ChannelIdentityEntity channelIdentity() {
        return channelIdentity;
    }

    public ConversationStatus status() {
        return status;
    }

    public AgentEntity assignedAgent() {
        return assignedAgent;
    }

    public Channel channel() {
        return channel;
    }

    public ConversationSourceType sourceType() {
        return sourceType;
    }

    public String providerAccountId() {
        return providerAccountId;
    }

    public String externalConversationId() {
        return externalConversationId;
    }

    public String subject() {
        return subject;
    }

    public String lastMessagePreview() {
        return lastMessagePreview;
    }

    public Instant lastActivityAt() {
        return lastActivityAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
