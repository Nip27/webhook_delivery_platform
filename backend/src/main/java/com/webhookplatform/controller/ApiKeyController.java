package com.webhookplatform.controller;

import com.webhookplatform.dto.request.CreateApiKeyRequest;
import com.webhookplatform.dto.response.ApiResponse;
import com.webhookplatform.dto.response.ApiKeyResponse;
import com.webhookplatform.service.ApiKeyService;
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
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateApiKeyRequest request) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("API key created successfully", apiKeyService.createApiKey(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys(@AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.listApiKeys(userId)));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID keyId) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        apiKeyService.revokeApiKey(keyId, userId);
        return ResponseEntity.ok(ApiResponse.success("API key revoked successfully", null));
    }
}
