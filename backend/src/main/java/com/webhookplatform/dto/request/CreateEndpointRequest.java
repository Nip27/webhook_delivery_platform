package com.webhookplatform.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
@Data
public class CreateEndpointRequest {
    @NotBlank(message = "URL is required")
    @URL(message = "Must be a valid URL")
    @Size(max = 2048)
    private String url;
    @Size(max = 500)
    private String description;
}
