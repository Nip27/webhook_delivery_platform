package com.webhookplatform.controller;

import com.webhookplatform.dto.response.ApiResponse;
import com.webhookplatform.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dead-letters")
@RequiredArgsConstructor
public class DeadLetterController {

    private final DeadLetterQueueService deadLetterQueueService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.min(limit, 200);
        List<Map<String, Object>> items = deadLetterQueueService.listDeadLetters(safeLimit);
        long total = deadLetterQueueService.queueSize();
        Map<String, Object> result = Map.of(
            "total", total,
            "returned", items.size(),
            "items", items
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/replay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replay(
            @RequestParam String eventId,
            @RequestParam String subscriptionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean replayed = deadLetterQueueService.replay(eventId, subscriptionId);
        if (replayed) {
            Map<String, Object> result = Map.of(
                "replayed", true,
                "eventId", eventId,
                "subscriptionId", subscriptionId
            );
            return ResponseEntity.ok(ApiResponse.success("DLQ event replayed successfully", result));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("DLQ entry not found for the given eventId and subscriptionId"));
    }
}
