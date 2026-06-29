package com.vdt2026.omnicare.inbox.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<AgentEntity, UUID> {

    Optional<AgentEntity> findByEmailIgnoreCase(String email);

    java.util.List<AgentEntity> findByActiveTrueOrderByDisplayNameAsc();
}
