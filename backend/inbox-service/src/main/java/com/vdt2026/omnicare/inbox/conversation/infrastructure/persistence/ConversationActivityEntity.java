package com.vdt2026.omnicare.inbox.conversation.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.conversation.domain.ConversationActivityType;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
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
@Table(name = "conversation_activities")
public class ConversationActivityEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_agent_id")
    private AgentEntity actorAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConversationActivityType activityType;

    @Column(length = 500)
    private String oldValue;

    @Column(length = 500)
    private String newValue;

    @Column(nullable = false)
    private Instant createdAt;

    protected ConversationActivityEntity() {
    }

    public ConversationActivityEntity(
        UUID id,
        ConversationEntity conversation,
        AgentEntity actorAgent,
        ConversationActivityType activityType,
        String oldValue,
        String newValue,
        Instant createdAt
    ) {
        this.id = id;
        this.conversation = conversation;
        this.actorAgent = actorAgent;
        this.activityType = activityType;
        this.oldValue = oldValue;
        this.newValue = newValue;
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

    public UUID id() {
        return id;
    }

    public ConversationActivityType activityType() {
        return activityType;
    }

    public AgentEntity actorAgent() {
        return actorAgent;
    }

    public String oldValue() {
        return oldValue;
    }

    public String newValue() {
        return newValue;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
