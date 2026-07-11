package com.webhookplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.dto.request.CreateEndpointRequest;
import com.webhookplatform.dto.response.WebhookEndpointResponse;
import com.webhookplatform.security.JwtAuthFilter;
import com.webhookplatform.security.JwtUtil;
import com.webhookplatform.service.WebhookEndpointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookEndpointController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for pure controller logic testing
public class WebhookEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebhookEndpointService endpointService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private com.webhookplatform.repository.ApiKeyRepository apiKeyRepository;

    @MockBean
    private com.webhookplatform.security.CustomUserDetailsService customUserDetailsService;


    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testCreateEndpoint() throws Exception {
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        CreateEndpointRequest request = new CreateEndpointRequest();
        request.setUrl("https://test.com");

        WebhookEndpointResponse mockResponse = WebhookEndpointResponse.builder()
            .id(UUID.randomUUID()).url("https://test.com").isActive(true).build();

        when(endpointService.createEndpoint(eq(specificUserId), any(CreateEndpointRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/webhooks/endpoints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Endpoint created successfully"))
            .andExpect(jsonPath("$.data.url").value("https://test.com"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testListEndpoints() throws Exception {
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        WebhookEndpointResponse r1 = WebhookEndpointResponse.builder().url("url1").build();

        when(endpointService.listEndpoints(specificUserId)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/webhooks/endpoints"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].url").value("url1"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testDeleteEndpoint() throws Exception {
        UUID specificUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID endpointId = UUID.randomUUID();

        doNothing().when(endpointService).deleteEndpoint(endpointId, specificUserId);

        mockMvc.perform(delete("/api/v1/webhooks/endpoints/" + endpointId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Endpoint deleted successfully"));
            
        verify(endpointService, times(1)).deleteEndpoint(endpointId, specificUserId);
    }
}
