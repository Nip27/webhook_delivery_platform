package com.webhookplatform.service;

import com.webhookplatform.dto.response.DashboardStatsResponse;
import java.util.UUID;

public interface DashboardService {
    DashboardStatsResponse getStats(UUID userId);
}
