package org.misha.authservice.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityRoutesTest.Probe.class)
@Import({SecurityConfig.class, JwtFilter.class, SecurityRoutesTest.Probe.class})
class SecurityRoutesTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtUtil jwt;
    @MockitoBean EmailPhoneAuthenticationProvider provider;

    @ParameterizedTest
    @ValueSource(strings = {"/api/clients", "/api/tools", "/api/categories", "/api/templates", "/api/rent",
            "/api/bookings", "/api/contracts", "/api/admin/documents", "/api/stats", "/api/v1/tools",
            "/api/v1/sync", "/api/admin/users", "/api/v1/admin/traders"})
    void crmRequiresAdministrator(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user("1").roles("CLIENT"))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(user("1").roles("TRADER"))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(user("1").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void traderSyncRequiresAuthenticatedTraderOrAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/sync/products")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/sync/products").with(user("1").roles("CLIENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/sync/products").with(user("1").roles("TRADER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/sync/products").with(user("1").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void authIsPublicAndProfileRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/api/profile/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profile/me").with(user("1").roles("TRADER"))).andExpect(status().isOk());
    }

    @Test void invalidBearerCannotAuthenticateAndValidSignedAdminCan() throws Exception {
        when(jwt.validateAccessClaims("invalid")).thenThrow(new io.jsonwebtoken.JwtException("invalid"));
        mvc.perform(get("/api/clients").header(HttpHeaders.AUTHORIZATION, "Bearer invalid")).andExpect(status().isUnauthorized());
        var claims = io.jsonwebtoken.Jwts.claims().setSubject("1"); claims.put("role", "ADMIN");
        when(jwt.validateAccessClaims("valid")).thenReturn(claims);
        mvc.perform(get("/api/clients").header(HttpHeaders.AUTHORIZATION, "Bearer valid")).andExpect(status().isOk());
    }

    @RestController
    static class Probe {
        @GetMapping({"/api/clients", "/api/tools", "/api/categories", "/api/templates", "/api/rent",
                "/api/bookings", "/api/contracts", "/api/admin/documents", "/api/stats", "/api/v1/tools",
                "/api/v1/sync", "/api/admin/users", "/api/v1/admin/traders", "/api/v1/admin/sync/products", "/api/profile/me"})
        String get() { return "ok"; }
        @PostMapping("/api/auth/login") String login() { return "ok"; }
    }
}
