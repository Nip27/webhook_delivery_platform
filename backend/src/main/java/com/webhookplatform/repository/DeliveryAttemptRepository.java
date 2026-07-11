package com.webhookplatform.repository;
import com.webhookplatform.entity.DeliveryAttempt;
import com.webhookplatform.entity.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.UUID;
@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {
    @Query("SELECT da FROM DeliveryAttempt da JOIN da.event e WHERE e.user.id = :userId ORDER BY da.createdAt DESC")
    Page<DeliveryAttempt> findByEventUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(da) FROM DeliveryAttempt da JOIN da.event e WHERE e.user.id = :userId AND (:status IS NULL OR da.status = :status) AND da.createdAt >= :since")
    long countByUserIdAndStatusSince(@Param("userId") UUID userId, @Param("status") DeliveryStatus status, @Param("since") LocalDateTime since);
}
