package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ProductSyncResponse;
import org.misha.authservice.entity.Product;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.TraderRepository;
import org.misha.authservice.service.ApiKeyValidationService;
import org.misha.authservice.service.ProductSyncService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminProductSyncController {
    private final ProductSyncService productSyncService;
    private final ApiKeyValidationService apiKeyValidationService;
    private final TraderRepository traderRepository;

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TRADER')")
    public ResponseEntity<ProductSyncResponse> syncProducts(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam(required = false) Long traderId,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) Long version,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Trader trader = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;

        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            isAdmin = authorities.stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        if (apiKey != null && !apiKey.isBlank()) {
            trader = apiKeyValidationService.validateApiKey(apiKey);
        } else {
            if (!isAdmin) {
                throw new AppException("API_KEY_REQUIRED", "X-API-KEY header is required for traders", HttpStatus.UNAUTHORIZED);
            }
            if (traderId != null) {
                trader = traderRepository.findById(traderId)
                        .orElseThrow(() -> new AppException("TRADER_NOT_FOUND", "Trader not found", HttpStatus.NOT_FOUND));
            } else {
                throw new AppException("TRADER_ID_OR_API_KEY_REQUIRED", "Either traderId parameter or X-API-KEY header is required", HttpStatus.BAD_REQUEST);
            }
        }

        if (traderId != null && trader != null && !trader.getId().equals(traderId)) {
            throw new AppException("TRADER_ID_MISMATCH", "Trader ID does not match API key", HttpStatus.FORBIDDEN);
        }

        Page<Product> productPage = productSyncService.syncProducts(trader, since, version, page, size);

        ProductSyncResponse response = ProductSyncResponse.builder()
                .content(productPage.getContent())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .hasNext(productPage.hasNext())
                .hasPrevious(productPage.hasPrevious())
                .build();

        return ResponseEntity.ok(response);
    }
}

