package org.misha.authservice.controller;

import org.junit.jupiter.api.Test;
import org.misha.authservice.service.*;
import org.misha.authservice.security.EmailPhoneAuthenticationProvider;
import org.misha.authservice.security.JwtFilter;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebMvcTest(
        controllers = SyncController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtFilter.class, EmailPhoneAuthenticationProvider.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class SyncControllerTest {

    @MockBean
    private IdempotencyService idempotencyService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private SyncService syncService;

    @Test
    void testContextLoads() {
        assertNotNull(mockMvc);
    }

    @Test
    void malformedParametersAndNullBatchItemsAreBadRequests() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/sync/pull"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/sync/contracts")
                .contentType("application/json").content("{\"creations\":[null]}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(syncService, idempotencyService);
    }
}
