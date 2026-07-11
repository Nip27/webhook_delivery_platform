package com.webhookplatform.util;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
@Data @NoArgsConstructor @AllArgsConstructor
public class DeliveryTask {
    private UUID eventId;
    private UUID subscriptionId;
    private int attemptNumber;
}
