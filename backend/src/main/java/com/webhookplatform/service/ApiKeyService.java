package com.webhookplatform.service;

import com.webhookplatform.dto.request.CreateApiKeyRequest;
import com.webhookplatform.dto.response.ApiKeyResponse;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {
    ApiKeyResponse createApiKey(UUID userId, CreateApiKeyRequest request);
    List<ApiKeyResponse> listApiKeys(UUID userId);
    void revokeApiKey(UUID keyId, UUID userId);
}
