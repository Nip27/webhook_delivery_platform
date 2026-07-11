package com.webhookplatform.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;
@Data
public class CreateSubscriptionRequest {
    @NotNull(message = "Endpoint ID is required")
    private UUID webhookEndpointId;
    @NotBlank(message = "Event type is required")
    private String eventTypeName;
}
