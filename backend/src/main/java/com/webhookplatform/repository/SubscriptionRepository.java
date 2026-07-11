package com.webhookplatform.repository;
import com.webhookplatform.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByEventTypeNameAndIsActiveTrue(String eventTypeName);
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByWebhookEndpointIdAndEventTypeName(UUID endpointId, String eventTypeName);
}
