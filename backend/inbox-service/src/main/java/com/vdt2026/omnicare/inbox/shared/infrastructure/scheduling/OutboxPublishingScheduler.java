package com.vdt2026.omnicare.inbox.shared.infrastructure.scheduling;

import com.vdt2026.omnicare.inbox.shared.application.OutboxPublishingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
class OutboxPublishingScheduler {

    private final OutboxPublishingService outboxPublishingService;

    OutboxPublishingScheduler(OutboxPublishingService outboxPublishingService) {
        this.outboxPublishingService = outboxPublishingService;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay-ms:5000}")
    void publishDueEvents() {
        outboxPublishingService.publishDueEvents();
    }
}
