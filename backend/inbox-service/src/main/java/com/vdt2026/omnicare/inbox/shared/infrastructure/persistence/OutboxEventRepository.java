package com.vdt2026.omnicare.inbox.shared.infrastructure.persistence;

import com.vdt2026.omnicare.inbox.shared.infrastructure.persistence.OutboxEventEntity.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
        OutboxStatus status,
        Instant availableAt,
        Pageable pageable
    );
}
