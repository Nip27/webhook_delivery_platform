package com.webhookplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.dto.request.PublishEventRequest;
import com.webhookplatform.dto.response.EventResponse;
import com.webhookplatform.dto.response.PageResponse;
import com.webhookplatform.entity.DeliveryAttempt;
import com.webhookplatform.entity.Event;
import com.webhookplatform.entity.Subscription;
import com.webhookplatform.entity.User;
import com.webhookplatform.entity.enums.DeliveryStatus;
import com.webhookplatform.entity.enums.EventStatus;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.metrics.WebhookMetrics;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.EventRepository;
import com.webhookplatform.repository.SubscriptionRepository;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.service.EventService;
import com.webhookplatform.util.DeliveryTask;
import com.webhookplatform.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookMetrics metrics;


    @Override
    @Transactional
    public EventResponse publishEvent(UUID userId, PublishEventRequest request) {
        if (request.getIdempotencyKey() != null) {
            String idempKey = RedisKeys.idempotencyKey(userId, request.getIdempotencyKey());
            Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.debug("Duplicate event rejected for idempotency key: {}", request.getIdempotencyKey());
                return eventRepository.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey())
                    .map(this::toResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Original event not found"));
            }
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        String payloadJson;
        try {
            payloadJson = request.getPayload() != null
                ? objectMapper.writeValueAsString(request.getPayload()) : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }

        Event event = Event.builder()
            .user(user).eventType(request.getEventType())
            .payload(payloadJson).status(EventStatus.PROCESSING)
            .idempotencyKey(request.getIdempotencyKey()).build();
        Event savedEvent = eventRepository.save(event);
        metrics.incrementEventsPublished();

        List<Subscription> subscriptions =
            subscriptionRepository.findByEventTypeNameAndIsActiveTrue(request.getEventType());

        if (subscriptions.isEmpty()) {
            log.debug("No subscriptions for event type: {}", request.getEventType());
            savedEvent.setStatus(EventStatus.DELIVERED);
            eventRepository.save(savedEvent);
            return toResponse(savedEvent);
        }

        for (Subscription subscription : subscriptions) {
            DeliveryAttempt attempt = DeliveryAttempt.builder()
                .event(savedEvent).subscription(subscription)
                .status(DeliveryStatus.PENDING).attemptNumber(1).build();
            deliveryAttemptRepository.save(attempt);
            DeliveryTask task = new DeliveryTask(savedEvent.getId(), subscription.getId(), 1);
            redisTemplate.opsForList().rightPush(RedisKeys.DELIVERY_QUEUE, task);
        }

        log.info("Event {} published: type={}, subscriptions={}",
            savedEvent.getId(), request.getEventType(), subscriptions.size());
        return toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> listEvents(UUID userId, int page, int size) {
        Page<Event> pageResult = eventRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, size));
        return PageResponse.<EventResponse>builder()
            .content(pageResult.getContent().stream().map(this::toResponse).toList())
            .page(pageResult.getNumber()).size(pageResult.getSize())
            .totalElements(pageResult.getTotalElements())
            .totalPages(pageResult.getTotalPages()).last(pageResult.isLast()).build();
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
            .id(event.getId()).eventType(event.getEventType())
            .status(event.getStatus().name()).idempotencyKey(event.getIdempotencyKey())
            .createdAt(event.getCreatedAt()).build();
    }
}
