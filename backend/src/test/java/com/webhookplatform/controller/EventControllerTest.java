package com.webhookplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.dto.request.PublishEventRequest;
import com.webhookplatform.dto.response.EventResponse;
import com.webhookplatform.dto.response.PageResponse;
import com.webhookplatform.entity.enums.EventStatus;
import com.webhookplatform.security.JwtAuthFilter;
import com.webhookplatform.security.JwtUtil;
import com.webhookplatform.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

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
        userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testPublishEvent() throws Exception {
        PublishEventRequest request = new PublishEventRequest();
        request.setEventType("order.created");
        request.setPayload("{\"id\": 1}");

        EventResponse response = EventResponse.builder()
            .id(UUID.randomUUID())
            .eventType("order.created")
            .status(EventStatus.PROCESSING.name())
            .build();

        when(eventService.publishEvent(eq(userId), any(PublishEventRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Event publishing accepted"))
            .andExpect(jsonPath("$.data.eventType").value("order.created"));
    }

    @Test
    @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000")
    void testListEvents() throws Exception {
        EventResponse response = EventResponse.builder()
            .id(UUID.randomUUID())
            .eventType("order.created")
            .status(EventStatus.DELIVERED.name())
            .createdAt(LocalDateTime.now())
            .build();

        PageResponse<EventResponse> page = PageResponse.<EventResponse>builder()
            .content(List.of(response))
            .totalElements(1L)
            .build();

        when(eventService.listEvents(userId, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/events")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].eventType").value("order.created"))
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
