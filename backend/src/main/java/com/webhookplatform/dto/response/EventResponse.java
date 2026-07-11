package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class EventResponse {
    private UUID id;
    private String eventType;
    private String status;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
