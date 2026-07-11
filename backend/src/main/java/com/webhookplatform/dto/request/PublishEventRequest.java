package com.webhookplatform.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class PublishEventRequest {
    @NotBlank(message = "Event type is required") @Size(max = 100)
    private String eventType;
    private Object payload;
    @Size(max = 100)
    private String idempotencyKey;
}
