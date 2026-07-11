package com.webhookplatform.service.impl;

import com.webhookplatform.dto.request.CreateSubscriptionRequest;
import com.webhookplatform.dto.response.SubscriptionResponse;
import com.webhookplatform.entity.Subscription;
import com.webhookplatform.entity.User;
import com.webhookplatform.entity.WebhookEndpoint;
import com.webhookplatform.exception.DuplicateResourceException;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.repository.SubscriptionRepository;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.repository.WebhookEndpointRepository;
import com.webhookplatform.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        WebhookEndpoint endpoint = endpointRepository
            .findByIdAndUserIdAndDeletedAtIsNull(request.getWebhookEndpointId(), userId)
            .orElseThrow(() -> ResourceNotFoundException.of("WebhookEndpoint", request.getWebhookEndpointId()));
        if (subscriptionRepository.existsByWebhookEndpointIdAndEventTypeName(
                request.getWebhookEndpointId(), request.getEventTypeName())) {
            throw new DuplicateResourceException("Subscription already exists for this endpoint and event type");
        }
        Subscription subscription = Subscription.builder()
            .user(user).webhookEndpoint(endpoint)
            .eventTypeName(request.getEventTypeName()).isActive(true).build();
        Subscription saved = subscriptionRepository.save(subscription);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> listSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSubscription(UUID subscriptionId, UUID userId) {
        Subscription subscription = subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
            .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
        subscriptionRepository.delete(subscription);
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return SubscriptionResponse.builder()
            .id(s.getId()).eventTypeName(s.getEventTypeName())
            .endpointId(s.getWebhookEndpoint().getId())
            .endpointUrl(s.getWebhookEndpoint().getUrl())
            .isActive(s.getIsActive()).createdAt(s.getCreatedAt()).build();
    }
}
