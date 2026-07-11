package com.webhookplatform.service;

import com.webhookplatform.dto.request.PublishEventRequest;
import com.webhookplatform.dto.response.EventResponse;
import com.webhookplatform.dto.response.PageResponse;

import java.util.UUID;

public interface EventService {
    EventResponse publishEvent(UUID userId, PublishEventRequest request);
    PageResponse<EventResponse> listEvents(UUID userId, int page, int size);
}
