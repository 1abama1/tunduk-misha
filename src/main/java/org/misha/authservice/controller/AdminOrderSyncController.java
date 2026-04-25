package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.OrderSyncResponse;
import org.misha.authservice.entity.Order;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.service.OrderSyncService;
import org.misha.authservice.service.SyncTraderResolver;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminOrderSyncController {
    private final OrderSyncService orderSyncService;
    private final SyncTraderResolver syncTraderResolver;

    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TRADER')")
    public ResponseEntity<OrderSyncResponse> syncOrders(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestParam(required = false) Long traderId,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) Long version,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Trader trader = syncTraderResolver.resolve(apiKey, traderId);

        Page<Order> orderPage = orderSyncService.syncOrders(trader, since, version, page, size);

        OrderSyncResponse response = OrderSyncResponse.builder()
                .content(orderPage.getContent())
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();

        return ResponseEntity.ok(response);
    }
}
