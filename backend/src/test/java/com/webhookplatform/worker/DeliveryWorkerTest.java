package com.webhookplatform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import com.webhookplatform.util.RedisKeys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeliveryWorker covering:
 * - Successful delivery
 * - Failed delivery (non-2xx response)
 * - Delivery error (exception)
 * - Retry scheduling
 * - Dead-lettering when max retries exhausted
 */
@ExtendWith(MockitoExtension.class)
class DeliveryWorkerTest {

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock EventRepository eventRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock HttpClient httpClient;
    @Mock DeadLetterQueueService deadLetterQueueService;
    @Mock ListOperations<String, Object> listOps;
    @Mock ZSetOperations<String, Object> zsetOps;
    @Mock HttpResponse<String> httpResponse;

    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    WebhookMetrics metrics;
    DeliveryWorker worker;

    UUID eventId;
    UUID subscriptionId;
    Event event;
    Subscription subscription;
    WebhookEndpoint endpoint;
    DeliveryTask task;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        metrics = new WebhookMetrics(new SimpleMeterRegistry());
        worker = new DeliveryWorker(
            redisTemplate,
            deliveryAttemptRepository,
            eventRepository,
            subscriptionRepository,
            httpClient,
            objectMapper,
            metrics,
            deadLetterQueueService
        );
        ReflectionTestUtils.setField(worker, "maxRetries", 3);
        ReflectionTestUtils.setField(worker, "httpTimeoutMs", 2000L);

        eventId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();

        endpoint = WebhookEndpoint.builder()
            .id(UUID.randomUUID())
            .url("https://example.com/hook")
            .secretHash("sh").secretPrefix("sp").isActive(true).build();

        event = Event.builder()
            .id(eventId).eventType("order.created")
            .payload("{\"orderId\":\"abc\"}")
            .status(EventStatus.PROCESSING)
            .createdAt(LocalDateTime.now()).build();

        subscription = Subscription.builder()
            .id(subscriptionId).webhookEndpoint(endpoint)
            .eventTypeName("order.created").isActive(true).build();

        task = new DeliveryTask(eventId, subscriptionId, 1);

        lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
    }

    // =========================================================================
    // Successful delivery
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void processDeliveryQueue_successfulDelivery_savesSuccessAttemptAndMarksDelivered()
            throws Exception {
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(task);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("ok");
        when(deliveryAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.processDeliveryQueue();

        ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(1);

        assertThat(event.getStatus()).isEqualTo(EventStatus.DELIVERED);
        verify(eventRepository).save(event);
        verifyNoInteractions(deadLetterQueueService);
    }

    // =========================================================================
    // Failed delivery (non-2xx)
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void processDeliveryQueue_non2xxResponse_schedulesRetry() throws Exception {
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(task);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("server error");
        when(deliveryAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.processDeliveryQueue();

        ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(captor.capture());
        // Saved as FAILED (retry scheduled, not dead-lettered yet)
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.FAILED);

        // Retry should be scheduled in sorted-set
        verify(zsetOps).addIfAbsent(eq(RedisKeys.RETRY_QUEUE), any(), anyDouble());
        verifyNoInteractions(deadLetterQueueService);
    }

    // =========================================================================
    // Failed delivery — exception (timeout, network error)
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void processDeliveryQueue_httpException_schedulesRetry() throws Exception {
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(task);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new java.io.IOException("Connection refused"));
        when(deliveryAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.processDeliveryQueue();

        ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("Connection refused");
        verify(zsetOps).addIfAbsent(eq(RedisKeys.RETRY_QUEUE), any(), anyDouble());
    }

    // =========================================================================
    // Dead-letter when max retries exhausted
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void processDeliveryQueue_maxRetriesExhausted_deadLetters() throws Exception {
        DeliveryTask finalAttemptTask = new DeliveryTask(eventId, subscriptionId, 3); // == maxRetries
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(finalAttemptTask);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpResponse.body()).thenReturn("unavailable");
        when(deliveryAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        worker.processDeliveryQueue();

        ArgumentCaptor<DeliveryAttempt> captor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeliveryStatus.DEAD_LETTERED);

        // Must enqueue to DLQ and must NOT schedule another retry
        verify(deadLetterQueueService).enqueue(finalAttemptTask);
        verify(zsetOps, never()).addIfAbsent(eq(RedisKeys.RETRY_QUEUE), any(), anyDouble());
    }

    // =========================================================================
    // Empty queue — no-op
    // =========================================================================

    @Test
    void processDeliveryQueue_emptyQueue_doesNothing() {
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(null);
        worker.processDeliveryQueue();
        verifyNoInteractions(eventRepository, deliveryAttemptRepository, httpClient);
    }

    // =========================================================================
    // Missing entity — task dropped safely
    // =========================================================================

    @Test
    void processDeliveryQueue_eventNotFound_dropsTask() {
        when(listOps.leftPop(RedisKeys.DELIVERY_QUEUE)).thenReturn(task);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));

        worker.processDeliveryQueue();

        verifyNoInteractions(httpClient, deliveryAttemptRepository);
    }
}
