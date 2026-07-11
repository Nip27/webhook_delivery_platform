package com.webhookplatform.service;

import com.webhookplatform.dto.request.CreateEndpointRequest;
import com.webhookplatform.dto.response.WebhookEndpointResponse;

import java.util.List;
import java.util.UUID;

public interface WebhookEndpointService {
    WebhookEndpointResponse createEndpoint(UUID userId, CreateEndpointRequest request);
    List<WebhookEndpointResponse> listEndpoints(UUID userId);
    void deleteEndpoint(UUID endpointId, UUID userId);
}
