package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class ApiKeyResponse {
    private UUID id;
    private String name;
    private String key;
    private String keyPrefix;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
