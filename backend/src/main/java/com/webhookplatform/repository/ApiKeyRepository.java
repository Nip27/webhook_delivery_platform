package com.webhookplatform.repository;
import com.webhookplatform.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHashAndIsActiveTrue(String keyHash);
    List<ApiKey> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);
    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);
}
