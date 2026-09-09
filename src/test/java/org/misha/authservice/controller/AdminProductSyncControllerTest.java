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
        controllers = AdminProductSyncController.class,
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
class AdminProductSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private ProductSyncService productSyncService;
    @MockBean
    private SyncTraderResolver syncTraderResolver;

    @Test
    void testContextLoads() {
        assertNotNull(mockMvc);
    }
}
