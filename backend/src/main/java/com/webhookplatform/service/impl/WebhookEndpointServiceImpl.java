package com.webhookplatform.service.impl;

import com.webhookplatform.dto.request.CreateEndpointRequest;
import com.webhookplatform.dto.response.WebhookEndpointResponse;
import com.webhookplatform.entity.User;
import com.webhookplatform.entity.WebhookEndpoint;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.repository.WebhookEndpointRepository;
import com.webhookplatform.service.WebhookEndpointService;
import com.webhookplatform.util.ApiKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEndpointServiceImpl implements WebhookEndpointService {

    private final WebhookEndpointRepository endpointRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WebhookEndpointResponse createEndpoint(UUID userId, CreateEndpointRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        String rawSecret = ApiKeyGenerator.generateKey().replace("whk_live_", "whk_sec_");
        String secretHash = ApiKeyGenerator.hashKey(rawSecret);
        String secretPrefix = ApiKeyGenerator.extractPrefix(rawSecret);
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
            .user(user).url(request.getUrl()).description(request.getDescription())
            .secretHash(secretHash).secretPrefix(secretPrefix).isActive(true).build();
        WebhookEndpoint saved = endpointRepository.save(endpoint);
        return WebhookEndpointResponse.builder()
            .id(saved.getId()).url(saved.getUrl()).description(saved.getDescription())
            .secretPrefix(secretPrefix).secret(rawSecret).isActive(true).createdAt(saved.getCreatedAt()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> listEndpoints(UUID userId) {
        return endpointRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
            .stream().map(ep -> WebhookEndpointResponse.builder()
                .id(ep.getId()).url(ep.getUrl()).description(ep.getDescription())
                .secretPrefix(ep.getSecretPrefix()).secret(null).isActive(ep.getIsActive())
                .createdAt(ep.getCreatedAt()).build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteEndpoint(UUID endpointId, UUID userId) {
        WebhookEndpoint endpoint = endpointRepository
            .findByIdAndUserIdAndDeletedAtIsNull(endpointId, userId)
            .orElseThrow(() -> ResourceNotFoundException.of("WebhookEndpoint", endpointId));
        endpoint.setDeletedAt(LocalDateTime.now());
        endpoint.setIsActive(false);
        endpointRepository.save(endpoint);
        log.info("Endpoint soft-deleted: {} for user {}", endpointId, userId);
    }
}
