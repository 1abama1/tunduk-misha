package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ProductSyncResponse;
import org.misha.authservice.entity.Product;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.service.ProductSyncService;
import org.misha.authservice.service.SyncTraderResolver;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminProductSyncController {
    private final ProductSyncService productSyncService;
    private final SyncTraderResolver syncTraderResolver;

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TRADER')")
    public ResponseEntity<ProductSyncResponse> syncProducts(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam(required = false) Long traderId,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) Long version,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Trader trader = syncTraderResolver.resolve(apiKey, traderId);

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
