package com.webhookplatform.service;

import com.webhookplatform.dto.response.DeliveryAttemptResponse;
import com.webhookplatform.dto.response.PageResponse;
import java.util.UUID;

public interface DeliveryService {
    PageResponse<DeliveryAttemptResponse> listDeliveries(UUID userId, int page, int size);
}
