package com.webhookplatform.controller;

import com.webhookplatform.dto.request.CreateSubscriptionRequest;
import com.webhookplatform.dto.response.ApiResponse;
import com.webhookplatform.dto.response.SubscriptionResponse;
import com.webhookplatform.service.SubscriptionService;
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
@RequestMapping("/api/v1/webhooks/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Subscription created successfully", subscriptionService.createSubscription(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> listSubscriptions(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.listSubscriptions(userId)));
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID subscriptionId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        subscriptionService.deleteSubscription(subscriptionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Subscription deleted successfully", null));
    }
}
