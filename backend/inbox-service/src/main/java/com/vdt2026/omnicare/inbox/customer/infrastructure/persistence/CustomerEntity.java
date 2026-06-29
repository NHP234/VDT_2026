package com.vdt2026.omnicare.inbox.customer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    protected CustomerEntity() {
    }

    public CustomerEntity(UUID id, String displayName, Instant createdAt) {
        this.id = id;
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

    public String displayName() {
        return displayName;
    }
}
