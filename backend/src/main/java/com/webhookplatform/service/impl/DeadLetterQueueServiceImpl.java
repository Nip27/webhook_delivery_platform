package com.webhookplatform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.service.DeadLetterQueueService;
import com.webhookplatform.util.DeliveryTask;
import com.webhookplatform.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueServiceImpl implements DeadLetterQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueue(DeliveryTask task) {
        double score = (double) (System.currentTimeMillis() / 1000L);
        redisTemplate.opsForZSet().add(RedisKeys.DEAD_LETTER_QUEUE, task, score);
        log.info("[DLQ] enqueued eventId={} subscriptionId={} attempt={}",
            task.getEventId(), task.getSubscriptionId(), task.getAttemptNumber());
    }

    @Override
    public List<Map<String, Object>> listDeadLetters(int limit) {
        Set<Object> items = redisTemplate.opsForZSet()
            .reverseRange(RedisKeys.DEAD_LETTER_QUEUE, 0, limit - 1);
        if (items == null || items.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object raw : items) {
            try {
                DeliveryTask task = objectMapper.convertValue(raw, DeliveryTask.class);
                Double score = redisTemplate.opsForZSet().score(RedisKeys.DEAD_LETTER_QUEUE, raw);
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("eventId", task.getEventId());
                entry.put("subscriptionId", task.getSubscriptionId());
                entry.put("attemptNumber", task.getAttemptNumber());
                entry.put("deadLetteredAt", score != null
                    ? LocalDateTime.ofEpochSecond(score.longValue(), 0,
                        java.time.ZoneOffset.UTC).toString()
                    : null);
                result.add(entry);
            } catch (Exception e) {
                log.warn("[DLQ] failed to deserialise DLQ entry: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean replay(String eventId, String subscriptionId) {
        UUID eId = UUID.fromString(eventId);
        UUID sId = UUID.fromString(subscriptionId);

        Set<Object> all = redisTemplate.opsForZSet()
            .range(RedisKeys.DEAD_LETTER_QUEUE, 0, -1);
        if (all == null) return false;

        for (Object raw : all) {
            try {
                DeliveryTask task = objectMapper.convertValue(raw, DeliveryTask.class);
                if (eId.equals(task.getEventId()) && sId.equals(task.getSubscriptionId())) {
                    DeliveryTask replayTask = new DeliveryTask(eId, sId, 1);
                    redisTemplate.execute(new SessionCallback<Object>() {
                        @Override
                        public <K, V> Object execute(RedisOperations<K, V> operations) {
                            operations.multi();
                            operations.opsForZSet().remove((K) RedisKeys.DEAD_LETTER_QUEUE, raw);
                            operations.opsForList().rightPush((K) RedisKeys.DELIVERY_QUEUE, (V) replayTask);
                            return operations.exec();
                        }
                    });
                    log.info("[DLQ] replaying eventId={} subscriptionId={}", eventId, subscriptionId);
                    return true;
                }
            } catch (Exception e) {
                log.warn("[DLQ] error scanning DLQ for replay: {}", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public long queueSize() {
        Long size = redisTemplate.opsForZSet().size(RedisKeys.DEAD_LETTER_QUEUE);
        return size != null ? size : 0L;
    }
}
