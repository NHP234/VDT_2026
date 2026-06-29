package com.vdt2026.omnicare.inbox.customer.infrastructure.persistence;

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
@Table(name = "channel_identities")
public class ChannelIdentityEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(nullable = false, length = 200)
    private String providerAccountId;

    @Column(nullable = false, length = 300)
    private String externalIdentityId;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    protected ChannelIdentityEntity() {
    }

    public ChannelIdentityEntity(
        UUID id,
        CustomerEntity customer,
        Channel channel,
        String providerAccountId,
        String externalIdentityId,
        String displayName,
        Instant createdAt
    ) {
        this.id = id;
        this.customer = customer;
        this.channel = channel;
        this.providerAccountId = providerAccountId;
        this.externalIdentityId = externalIdentityId;
        this.displayName = displayName;
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

    public CustomerEntity customer() {
        return customer;
    }

    public Channel channel() {
        return channel;
    }

    public String providerAccountId() {
        return providerAccountId;
    }

    public String externalIdentityId() {
        return externalIdentityId;
    }

    public String displayName() {
        return displayName;
    }
}
