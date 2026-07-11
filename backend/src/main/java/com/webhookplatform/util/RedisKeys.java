package com.webhookplatform.util;
import java.util.UUID;
public final class RedisKeys {
    private RedisKeys() {}
    public static final String DELIVERY_QUEUE = "webhook:delivery:queue";
    public static final String RETRY_QUEUE = "webhook:retry:queue";
    public static final String DEAD_LETTER_QUEUE = "webhook:dead-letter:queue";
    public static String idempotencyKey(UUID userId, String idempotencyKey) {
        return "webhook:idempotency:" + userId + ":" + idempotencyKey;
    }
    public static String rateLimitKey(UUID userId, long windowTimestamp) {
        return "webhook:ratelimit:" + userId + ":" + windowTimestamp;
    }
}
