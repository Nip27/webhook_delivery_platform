package com.webhookplatform.dto.response;
import lombok.Builder;
import lombok.Data;
@Data @Builder
public class DashboardStatsResponse {
    private long totalDeliveries;
    private long successfulDeliveries;
    private long failedDeliveries;
    private long deadLetteredDeliveries;
    private long totalEvents;
    private double successRate;
}
