package com.webhookplatform.service;

import com.webhookplatform.dto.request.CreateEndpointRequest;
import com.webhookplatform.dto.response.WebhookEndpointResponse;
import com.webhookplatform.entity.User;
import com.webhookplatform.entity.WebhookEndpoint;
import com.webhookplatform.exception.ResourceNotFoundException;
import com.webhookplatform.repository.UserRepository;
import com.webhookplatform.repository.WebhookEndpointRepository;
import com.webhookplatform.service.impl.WebhookEndpointServiceImpl;
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
public class WebhookEndpointServiceTest {

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebhookEndpointServiceImpl endpointService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder().id(userId).email("test@example.com").build();
    }

    @Test
    void testCreateEndpoint_Success() {
        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setUrl("https://example.com/webhook");
        request.setDescription("Test Endpoint");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(endpointRepository.save(any(WebhookEndpoint.class))).thenAnswer(i -> {
            WebhookEndpoint ep = i.getArgument(0);
            ep.setId(UUID.randomUUID());
            return ep;
        });

        WebhookEndpointResponse response = endpointService.createEndpoint(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(response.getSecret()).isNotNull();
        assertThat(response.getSecretPrefix()).isNotNull();

        ArgumentCaptor<WebhookEndpoint> captor = ArgumentCaptor.forClass(WebhookEndpoint.class);
        verify(endpointRepository).save(captor.capture());
        WebhookEndpoint saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(saved.getSecretHash()).isNotNull();
    }

    @Test
    void testCreateEndpoint_UserNotFound() {
        CreateEndpointRequest request = new CreateEndpointRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> endpointService.createEndpoint(userId, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testListEndpoints() {
        WebhookEndpoint ep1 = WebhookEndpoint.builder().id(UUID.randomUUID()).url("url1").isActive(true).build();
        WebhookEndpoint ep2 = WebhookEndpoint.builder().id(UUID.randomUUID()).url("url2").isActive(true).build();

        when(endpointRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(ep1, ep2));

        List<WebhookEndpointResponse> responses = endpointService.listEndpoints(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getUrl()).isEqualTo("url1");
        assertThat(responses.get(0).getSecret()).isNull();
    }

    @Test
    void testDeleteEndpoint_Success() {
        UUID endpointId = UUID.randomUUID();
        WebhookEndpoint endpoint = WebhookEndpoint.builder().id(endpointId).user(testUser).isActive(true).build();

        when(endpointRepository.findByIdAndUserIdAndDeletedAtIsNull(endpointId, userId)).thenReturn(Optional.of(endpoint));

        endpointService.deleteEndpoint(endpointId, userId);

        assertThat(endpoint.getIsActive()).isFalse();
        assertThat(endpoint.getDeletedAt()).isNotNull();
        verify(endpointRepository).save(endpoint);
    }

    @Test
    void testDeleteEndpoint_NotFound() {
        UUID endpointId = UUID.randomUUID();
        when(endpointRepository.findByIdAndUserIdAndDeletedAtIsNull(endpointId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> endpointService.deleteEndpoint(endpointId, userId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
