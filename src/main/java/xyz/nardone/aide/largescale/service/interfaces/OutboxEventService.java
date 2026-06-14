package xyz.nardone.aide.largescale.service.interfaces;

import xyz.nardone.aide.largescale.constant.EOutboxEventType;
import java.time.LocalDateTime;
import java.util.Map;

public interface OutboxEventService {

    void createEvent(EOutboxEventType eventType, Map<String, Object> payload);

    long deleteProcessedEventsOlderThan(LocalDateTime processedBefore);

    void processPendingGraphEvents();

    void processPendingDashboardEvents();
}
