package com.webhookplatform.controller;

import com.webhookplatform.dto.request.CreateEndpointRequest;
import com.webhookplatform.dto.response.ApiResponse;
import com.webhookplatform.dto.response.WebhookEndpointResponse;
import com.webhookplatform.service.WebhookEndpointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/endpoints")
@RequiredArgsConstructor
public class WebhookEndpointController {

    private final WebhookEndpointService endpointService;

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookEndpointResponse>> createEndpoint(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateEndpointRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Endpoint created successfully", endpointService.createEndpoint(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEndpointResponse>>> listEndpoints(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(endpointService.listEndpoints(userId)));
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<ApiResponse<Void>> deleteEndpoint(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID endpointId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        endpointService.deleteEndpoint(endpointId, userId);
        return ResponseEntity.ok(ApiResponse.success("Endpoint deleted successfully", null));
    }
}
