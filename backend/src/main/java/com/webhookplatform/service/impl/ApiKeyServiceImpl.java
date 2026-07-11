package com.webhookplatform.service.impl;

import com.webhookplatform.dto.request.CreateApiKeyRequest;
import com.webhookplatform.dto.response.ApiKeyResponse;
import com.webhookplatform.entity.ApiKey;
import com.webhookplatform.entity.User;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.repository.ApiKeyRepository;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.service.ApiKeyService;
import com.webhookplatform.util.ApiKeyGenerator;
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
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApiKeyResponse createApiKey(UUID userId, CreateApiKeyRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        String rawKey = ApiKeyGenerator.generateKey();
        String keyHash = ApiKeyGenerator.hashKey(rawKey);
        String keyPrefix = ApiKeyGenerator.extractPrefix(rawKey);
        ApiKey apiKey = ApiKey.builder()
            .user(user).name(request.getName())
            .keyHash(keyHash).keyPrefix(keyPrefix).isActive(true).build();
        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API key created for user {}: {}", userId, keyPrefix);
        return ApiKeyResponse.builder()
            .id(saved.getId()).name(saved.getName()).key(rawKey)
            .keyPrefix(keyPrefix).isActive(true).createdAt(saved.getCreatedAt()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID userId) {
        return apiKeyRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
            .stream().map(key -> ApiKeyResponse.builder()
                .id(key.getId()).name(key.getName()).key(null)
                .keyPrefix(key.getKeyPrefix()).isActive(key.getIsActive())
                .createdAt(key.getCreatedAt()).lastUsedAt(key.getLastUsedAt()).build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID keyId, UUID userId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(keyId, userId)
            .orElseThrow(() -> ResourceNotFoundException.of("ApiKey", keyId));
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("API key revoked: {} for user {}", keyId, userId);
    }
}
