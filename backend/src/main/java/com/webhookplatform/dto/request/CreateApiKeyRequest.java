package com.webhookplatform.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CreateApiKeyRequest {
    @NotBlank(message = "API key name is required") @Size(max = 100)
    private String name;
}
