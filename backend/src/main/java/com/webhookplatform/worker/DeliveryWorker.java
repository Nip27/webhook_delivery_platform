package com.webhookplatform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.entity.DeliveryAttempt;
import com.webhookplatform.entity.Event;
import com.webhookplatform.entity.Subscription;
import com.webhookplatform.entity.WebhookEndpoint;
import com.webhookplatform.entity.enums.DeliveryStatus;
import com.webhookplatform.entity.enums.EventStatus;
import com.webhookplatform.metrics.WebhookMetrics;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.EventRepository;
import com.webhookplatform.repository.SubscriptionRepository;
import com.webhookplatform.service.DeadLetterQueueService;
import com.webhookplatform.util.DeliveryTask;
import com.webhookplatform.util.HmacUtil;
import com.webhookplatform.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "delivery.workers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DeliveryWorker {


    private final RedisTemplate<String, Object> redisTemplate;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final WebhookMetrics metrics;
    private final DeadLetterQueueService deadLetterQueueService;

    @Value("${delivery.max-retries:5}")
    private int maxRetries;

    @Value("${delivery.http-timeout-ms:5000}")
    private long httpTimeoutMs;

    private final Random random = new Random();

    @Scheduled(fixedDelayString = "${delivery.worker-poll-interval-ms:1000}")
    public void processDeliveryQueue() {
        try {
            Object rawTask = redisTemplate.opsForList().leftPop(RedisKeys.DELIVERY_QUEUE);
            if (rawTask == null) return;
            DeliveryTask task = objectMapper.convertValue(rawTask, DeliveryTask.class);
            processTask(task);
        } catch (Exception e) {
            log.error("DeliveryWorker error", e);
        }
    }

    private void processTask(DeliveryTask task) {
        log.debug("Processing delivery task: eventId={}, subscriptionId={}, attempt={}",
            task.getEventId(), task.getSubscriptionId(), task.getAttemptNumber());

        Event event = eventRepository.findById(task.getEventId()).orElse(null);
        Subscription subscription = subscriptionRepository.findById(task.getSubscriptionId()).orElse(null);

        if (event == null || subscription == null) {
            log.warn("Event or subscription not found, skipping task: {}", task);
            return;
        }

        WebhookEndpoint endpoint = subscription.getWebhookEndpoint();
        String requestBody = buildRequestBody(event, task.getAttemptNumber());
        String signature = HmacUtil.computeSignature(endpoint.getSecretPrefix(), requestBody);

        long startTime = System.currentTimeMillis();
        DeliveryStatus status;
        Integer responseCode = null;
        String responseBody = null;
        String errorMessage = null;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .timeout(Duration.ofMillis(httpTimeoutMs))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", signature)
                .header("X-Webhook-Event-Type", event.getEventType())
                .header("X-Webhook-Delivery-Id", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseCode = response.statusCode();
            responseBody = response.body();

            if (responseCode >= 200 && responseCode < 300) {
                status = DeliveryStatus.SUCCESS;
                metrics.incrementDeliveriesSuccess();
                log.info("Delivery SUCCESS: event={}, endpoint={}, status={}", event.getId(), endpoint.getUrl(), responseCode);
            } else {
                status = DeliveryStatus.FAILED;
                metrics.incrementDeliveriesFailed();
                log.warn("Delivery FAILED: event={}, endpoint={}, status={}", event.getId(), endpoint.getUrl(), responseCode);
            }
        } catch (Exception e) {
            status = DeliveryStatus.FAILED;
            metrics.incrementDeliveriesFailed();
            errorMessage = e.getMessage();
            log.warn("Delivery ERROR: event={}, endpoint={}, error={}", event.getId(), endpoint.getUrl(), e.getMessage());
        }

        long durationMs = System.currentTimeMillis() - startTime;

        if (status == DeliveryStatus.FAILED) {
            if (task.getAttemptNumber() < maxRetries) {
                scheduleRetry(task, task.getAttemptNumber());
            } else {
                status = DeliveryStatus.DEAD_LETTERED;
                metrics.incrementDeadLetter();
                deadLetterQueueService.enqueue(task);
                log.warn("Max retries reached for event={}, subscription={}", event.getId(), subscription.getId());
            }
        }

        DeliveryAttempt attempt = DeliveryAttempt.builder()
            .event(event).subscription(subscription).status(status)
            .responseStatusCode(responseCode)
            .responseBody(responseBody != null && responseBody.length() > 1000
                ? responseBody.substring(0, 1000) : responseBody)
            .attemptNumber(task.getAttemptNumber()).durationMs(durationMs)
            .errorMessage(errorMessage).build();

        deliveryAttemptRepository.save(attempt);

        if (status == DeliveryStatus.SUCCESS) {
            event.setStatus(EventStatus.DELIVERED);
            eventRepository.save(event);
        }
    }

    private void scheduleRetry(DeliveryTask task, int currentAttempt) {
        long baseDelaySeconds = 30L * (long) Math.pow(2, currentAttempt - 1);
        double jitter = 0.8 + (random.nextDouble() * 0.4);
        long delaySeconds = (long) (baseDelaySeconds * jitter);
        double retryAt = (double) (System.currentTimeMillis() / 1000L) + delaySeconds;
        DeliveryTask retryTask = new DeliveryTask(task.getEventId(), task.getSubscriptionId(), currentAttempt + 1);
        redisTemplate.opsForZSet().addIfAbsent(RedisKeys.RETRY_QUEUE, retryTask, retryAt);
        metrics.incrementRetriesScheduled();
        log.debug("Retry scheduled for event={} at T+{}s (attempt {})", task.getEventId(), delaySeconds, currentAttempt + 1);
    }

    private String buildRequestBody(Event event, int attemptNumber) {
        try {
            Map<String, Object> body = Map.of(
                "id", UUID.randomUUID().toString(),
                "eventId", event.getId().toString(),
                "eventType", event.getEventType(),
                "attempt", attemptNumber,
                "timestamp", event.getCreatedAt().toString(),
                "data", event.getPayload() != null ? objectMapper.readValue(event.getPayload(), Object.class) : Map.of()
            );
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build request body", e);
        }
    }
}
