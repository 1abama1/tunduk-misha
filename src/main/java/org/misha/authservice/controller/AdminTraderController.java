package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ApiKeyResponse;
import org.misha.authservice.service.TraderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/traders")
@RequiredArgsConstructor
public class AdminTraderController {
    private final TraderService traderService;

    @PostMapping("/{id}/api-key")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiKeyResponse> generateApiKey(@PathVariable Long id) {
        String apiKey = traderService.generateApiKey(id);
        ApiKeyResponse response = ApiKeyResponse.builder()
                .apiKey(apiKey)
                .traderId(id)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/api-key")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiKeyResponse> getApiKey(@PathVariable Long id) {
        String apiKey = traderService.getApiKey(id);
        ApiKeyResponse response = ApiKeyResponse.builder()
                .apiKey(apiKey)
                .traderId(id)
                .build();
        return ResponseEntity.ok(response);
    }
}

