package com.webhookplatform.service;

import com.webhookplatform.dto.request.CreateApiKeyRequest;
import com.webhookplatform.dto.response.ApiKeyResponse;
import com.webhookplatform.entity.ApiKey;
import com.webhookplatform.entity.User;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.repository.ApiKeyRepository;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.service.impl.ApiKeyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder().id(userId).email("test@example.com").build();
    }

    @Test
    void testCreateApiKey_Success() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("Test Key");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> {
            ApiKey key = i.getArgument(0);
            key.setId(UUID.randomUUID());
            return key;
        });

        ApiKeyResponse response = apiKeyService.createApiKey(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Key");
        assertThat(response.getKey()).isNotNull();
        assertThat(response.getKeyPrefix()).isNotNull();
        assertThat(response.getIsActive()).isTrue();

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getName()).isEqualTo("Test Key");
        assertThat(saved.getKeyHash()).isNotNull();
    }

    @Test
    void testCreateApiKey_UserNotFound() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.createApiKey(userId, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void testListApiKeys() {
        ApiKey key1 = ApiKey.builder().id(UUID.randomUUID()).name("Key 1").keyPrefix("prefix1").isActive(true).build();
        ApiKey key2 = ApiKey.builder().id(UUID.randomUUID()).name("Key 2").keyPrefix("prefix2").isActive(true).build();

        when(apiKeyRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(key1, key2));

        List<ApiKeyResponse> responses = apiKeyService.listApiKeys(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Key 1");
        assertThat(responses.get(1).getName()).isEqualTo("Key 2");
        assertThat(responses.get(0).getKey()).isNull();
    }

    @Test
    void testRevokeApiKey_Success() {
        UUID keyId = UUID.randomUUID();
        ApiKey apiKey = ApiKey.builder().id(keyId).user(testUser).isActive(true).build();

        when(apiKeyRepository.findByIdAndUserId(keyId, userId)).thenReturn(Optional.of(apiKey));

        apiKeyService.revokeApiKey(keyId, userId);

        assertThat(apiKey.getIsActive()).isFalse();
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void testRevokeApiKey_NotFound() {
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findByIdAndUserId(keyId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.revokeApiKey(keyId, userId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
