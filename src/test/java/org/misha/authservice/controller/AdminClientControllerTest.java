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
        controllers = AdminClientController.class,
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
class AdminClientControllerTest {

    @MockBean
    private IdempotencyService idempotencyService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private AdminClientService adminClientService;
    @MockBean
    private ClientService clientService;
    @MockBean
    private ClientCardService clientCardService;
    @MockBean
    private ClientImageService clientImageService;
    @MockBean
    private ExcelClientImportService excelClientImportService;

    @Test
    void testContextLoads() {
        assertNotNull(mockMvc);
    }

    @Test
    void uploadedActiveContentIsDownloadedWithSafeHeaders() throws Exception {
        org.mockito.Mockito.when(clientImageService.getImage(7L)).thenReturn(
                org.misha.authservice.entity.ClientImage.builder().fileName("unsafe\r\n.html")
                        .fileType("text/html").data("<script>alert(1)</script>".getBytes(java.nio.charset.StandardCharsets.UTF_8)).build());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/clients/images/7"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("application/octet-stream"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
    }
}
