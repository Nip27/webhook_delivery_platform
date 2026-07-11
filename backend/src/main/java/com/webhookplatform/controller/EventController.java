package com.webhookplatform.controller;

import com.webhookplatform.dto.request.PublishEventRequest;
import com.webhookplatform.dto.response.ApiResponse;
import com.webhookplatform.dto.response.EventResponse;
import com.webhookplatform.dto.response.PageResponse;
import com.webhookplatform.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PublishEventRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success("Event publishing accepted", eventService.publishEvent(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(eventService.listEvents(userId, page, size)));
    }
}
