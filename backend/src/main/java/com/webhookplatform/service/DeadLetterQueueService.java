package com.webhookplatform.service;

import com.webhookplatform.util.DeliveryTask;
import java.util.List;
import java.util.Map;

public interface DeadLetterQueueService {
    void enqueue(DeliveryTask task);
    List<Map<String, Object>> listDeadLetters(int limit);
    boolean replay(String eventId, String subscriptionId);
    long queueSize();
}
