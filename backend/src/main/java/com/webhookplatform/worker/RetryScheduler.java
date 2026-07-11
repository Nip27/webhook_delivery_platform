package com.webhookplatform.worker;

import com.webhookplatform.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@ConditionalOnProperty(name = "delivery.workers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RetryScheduler {

    private final RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedDelay = 10000)
    public void processRetryQueue() {
        try {
            double now = System.currentTimeMillis() / 1000.0;
            Set<Object> dueTasks = redisTemplate.opsForZSet().rangeByScore(RedisKeys.RETRY_QUEUE, 0, now);
            if (dueTasks == null || dueTasks.isEmpty()) return;
            log.info("Moving {} retry tasks to delivery queue", dueTasks.size());
            for (Object task : dueTasks) {
                redisTemplate.execute(new SessionCallback<Object>() {
                    @Override
                    public <K, V> Object execute(RedisOperations<K, V> operations) {
                        operations.multi();
                        operations.opsForZSet().remove((K) RedisKeys.RETRY_QUEUE, task);
                        operations.opsForList().rightPush((K) RedisKeys.DELIVERY_QUEUE, (V) task);
                        return operations.exec();
                    }
                });
            }
        } catch (Exception e) {
            log.error("RetryScheduler error", e);
        }
    }
}
