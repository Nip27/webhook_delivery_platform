package com.webhookplatform.service;

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
import com.webhookplatform.service.impl.SubscriptionServiceImpl;
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
public class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User testUser;
    private WebhookEndpoint testEndpoint;
    private UUID userId;
    private UUID endpointId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        endpointId = UUID.randomUUID();
        testUser = User.builder().id(userId).email("test@example.com").build();
        testEndpoint = WebhookEndpoint.builder().id(endpointId).user(testUser).url("https://test").build();
    }

    @Test
    void testCreateSubscription_Success() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setWebhookEndpointId(endpointId);
        request.setEventTypeName("user.created");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(endpointRepository.findByIdAndUserIdAndDeletedAtIsNull(endpointId, userId))
            .thenReturn(Optional.of(testEndpoint));
        when(subscriptionRepository.existsByWebhookEndpointIdAndEventTypeName(endpointId, "user.created"))
            .thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription sub = i.getArgument(0);
            sub.setId(UUID.randomUUID());
            return sub;
        });

        SubscriptionResponse response = subscriptionService.createSubscription(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getEventTypeName()).isEqualTo("user.created");
        assertThat(response.getEndpointId()).isEqualTo(endpointId);
        assertThat(response.getIsActive()).isTrue();

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getWebhookEndpoint()).isEqualTo(testEndpoint);
        assertThat(saved.getEventTypeName()).isEqualTo("user.created");
    }

    @Test
    void testCreateSubscription_Duplicate() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setWebhookEndpointId(endpointId);
        request.setEventTypeName("user.created");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(endpointRepository.findByIdAndUserIdAndDeletedAtIsNull(endpointId, userId))
            .thenReturn(Optional.of(testEndpoint));
        when(subscriptionRepository.existsByWebhookEndpointIdAndEventTypeName(endpointId, "user.created"))
            .thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.createSubscription(userId, request))
            .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void testListSubscriptions() {
        Subscription sub = Subscription.builder()
            .id(UUID.randomUUID()).user(testUser).webhookEndpoint(testEndpoint)
            .eventTypeName("user.created").isActive(true).build();

        when(subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(sub));

        List<SubscriptionResponse> responses = subscriptionService.listSubscriptions(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getEventTypeName()).isEqualTo("user.created");
    }

    @Test
    void testDeleteSubscription_Success() {
        UUID subId = UUID.randomUUID();
        Subscription sub = Subscription.builder().id(subId).user(testUser).build();

        when(subscriptionRepository.findByIdAndUserId(subId, userId)).thenReturn(Optional.of(sub));

        subscriptionService.deleteSubscription(subId, userId);

        verify(subscriptionRepository).delete(sub);
    }
}
