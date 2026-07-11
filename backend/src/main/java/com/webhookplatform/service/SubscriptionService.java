package com.webhookplatform.service;

import com.webhookplatform.dto.request.CreateSubscriptionRequest;
import com.webhookplatform.dto.response.SubscriptionResponse;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request);
    List<SubscriptionResponse> listSubscriptions(UUID userId);
    void deleteSubscription(UUID subscriptionId, UUID userId);
}
