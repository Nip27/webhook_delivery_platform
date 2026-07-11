package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class SubscriptionResponse {
    private UUID id;
    private String eventTypeName;
    private UUID endpointId;
    private String endpointUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
