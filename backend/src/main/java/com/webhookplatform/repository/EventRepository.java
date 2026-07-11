package com.webhookplatform.repository;
import com.webhookplatform.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Event> findByIdAndUserId(UUID id, UUID userId);
    Optional<Event> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
