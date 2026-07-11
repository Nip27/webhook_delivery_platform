package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class WebhookEndpointResponse {
    private UUID id;
    private String url;
    private String description;
    private String secretPrefix;
    private String secret;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
