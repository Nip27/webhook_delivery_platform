package com.webhookplatform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Centralised Micrometer counters for webhook delivery platform.
 * All counters are created eagerly so they appear in /actuator/metrics
 * even before the first event is processed.
 */
@Component
public class WebhookMetrics {

    private final Counter eventsPublishedTotal;
    private final Counter deliveriesSuccessTotal;
    private final Counter deliveriesFailedTotal;
    private final Counter retriesScheduledTotal;
    private final Counter deadLetterTotal;

    public WebhookMetrics(MeterRegistry registry) {
        this.eventsPublishedTotal = Counter.builder("events_published_total")
            .description("Total number of events published")
            .register(registry);

        this.deliveriesSuccessTotal = Counter.builder("deliveries_success_total")
            .description("Total number of successful webhook deliveries")
            .register(registry);

        this.deliveriesFailedTotal = Counter.builder("deliveries_failed_total")
            .description("Total number of failed webhook delivery attempts")
            .register(registry);

        this.retriesScheduledTotal = Counter.builder("retries_scheduled_total")
            .description("Total number of delivery retries scheduled")
            .register(registry);

        this.deadLetterTotal = Counter.builder("dead_letter_total")
            .description("Total number of deliveries moved to dead-letter queue")
            .register(registry);
    }

    public void incrementEventsPublished() {
        eventsPublishedTotal.increment();
    }

    public void incrementDeliveriesSuccess() {
        deliveriesSuccessTotal.increment();
    }

    public void incrementDeliveriesFailed() {
        deliveriesFailedTotal.increment();
    }

    public void incrementRetriesScheduled() {
        retriesScheduledTotal.increment();
    }

    public void incrementDeadLetter() {
        deadLetterTotal.increment();
    }
}
