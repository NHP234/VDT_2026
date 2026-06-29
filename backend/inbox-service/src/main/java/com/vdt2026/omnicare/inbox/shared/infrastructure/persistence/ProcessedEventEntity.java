package com.vdt2026.omnicare.inbox.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String eventId;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(length = 120)
    private String correlationId;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
    }

    public ProcessedEventEntity(UUID id, String eventId, String eventType, String source, String correlationId, Instant processedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.source = source;
        this.correlationId = correlationId;
        this.processedAt = processedAt;
    }

    @PrePersist
    void beforePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }

    public String eventId() {
        return eventId;
    }
}
