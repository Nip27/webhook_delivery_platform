package com.webhookplatform.health;

import com.webhookplatform.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("webhookQueues")
@RequiredArgsConstructor
public class QueueHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            Long deliveryQueueSize = redisTemplate.opsForList().size(RedisKeys.DELIVERY_QUEUE);
            Long retryQueueSize = redisTemplate.opsForZSet().size(RedisKeys.RETRY_QUEUE);
            Long deadLetterQueueSize = redisTemplate.opsForZSet().size(RedisKeys.DEAD_LETTER_QUEUE);

            return Health.up()
                .withDetail("deliveryQueueSize", deliveryQueueSize != null ? deliveryQueueSize : 0)
                .withDetail("retryQueueSize", retryQueueSize != null ? retryQueueSize : 0)
                .withDetail("deadLetterQueueSize", deadLetterQueueSize != null ? deadLetterQueueSize : 0)
                .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
