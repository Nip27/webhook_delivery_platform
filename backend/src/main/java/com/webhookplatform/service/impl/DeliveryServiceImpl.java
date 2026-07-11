package com.webhookplatform.service.impl;

import com.webhookplatform.dto.response.DeliveryAttemptResponse;
import com.webhookplatform.dto.response.PageResponse;
import com.webhookplatform.entity.DeliveryAttempt;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DeliveryAttemptResponse> listDeliveries(UUID userId, int page, int size) {
        Page<DeliveryAttempt> pageResult = deliveryAttemptRepository.findByEventUserId(
            userId, PageRequest.of(page, size));
        return PageResponse.<DeliveryAttemptResponse>builder()
            .content(pageResult.getContent().stream().map(this::toResponse).collect(Collectors.toList()))
            .page(pageResult.getNumber()).size(pageResult.getSize())
            .totalElements(pageResult.getTotalElements())
            .totalPages(pageResult.getTotalPages()).last(pageResult.isLast()).build();
    }

    private DeliveryAttemptResponse toResponse(DeliveryAttempt da) {
        return DeliveryAttemptResponse.builder()
            .id(da.getId()).eventId(da.getEvent().getId())
            .eventType(da.getEvent().getEventType())
            .endpointUrl(da.getSubscription().getWebhookEndpoint().getUrl())
            .status(da.getStatus().name()).responseStatusCode(da.getResponseStatusCode())
            .responseBody(da.getResponseBody()).attemptNumber(da.getAttemptNumber())
            .durationMs(da.getDurationMs()).errorMessage(da.getErrorMessage())
            .createdAt(da.getCreatedAt()).build();
    }
}
