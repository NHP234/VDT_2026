package com.vdt2026.omnicare.inbox.shared.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    Optional<ProcessedEventEntity> findByEventId(String eventId);
}
