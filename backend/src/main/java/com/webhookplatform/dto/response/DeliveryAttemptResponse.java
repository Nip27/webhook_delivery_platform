package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;
@Data @Builder
public class DeliveryAttemptResponse {
    private UUID id;
    private UUID eventId;
    private String eventType;
    private String endpointUrl;
    private String status;
    private Integer responseStatusCode;
    private String responseBody;
    private Integer attemptNumber;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
