package com.webhookplatform.service.impl;

import com.webhookplatform.dto.response.DashboardStatsResponse;
import com.webhookplatform.entity.enums.DeliveryStatus;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.EventRepository;
import com.webhookplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(UUID userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long successful = deliveryAttemptRepository.countByUserIdAndStatusSince(userId, DeliveryStatus.SUCCESS, since);
        long failed = deliveryAttemptRepository.countByUserIdAndStatusSince(userId, DeliveryStatus.FAILED, since);
        long deadLettered = deliveryAttemptRepository.countByUserIdAndStatusSince(userId, DeliveryStatus.DEAD_LETTERED, since);
        long totalAttempts = successful + failed + deadLettered;
        double successRate = totalAttempts > 0 ? (double) successful / totalAttempts * 100.0 : 0.0;
        long totalEvents = eventRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1)).getTotalElements();
        return DashboardStatsResponse.builder()
            .totalDeliveries(totalAttempts).successfulDeliveries(successful)
            .failedDeliveries(failed).deadLetteredDeliveries(deadLettered)
            .totalEvents(totalEvents).successRate(Math.round(successRate * 10.0) / 10.0).build();
    }
}
