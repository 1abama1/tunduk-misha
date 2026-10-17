package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Order;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OrderSyncService {
    private final OrderRepository orderRepository;

    public Page<Order> syncOrders(Trader trader, String since, Long version, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), org.springframework.data.domain.Sort.by("id"));

        if (version != null) {
            throw new org.misha.authservice.exception.BadRequestException("Per-record version cannot be used as a sync cursor; request a full sync without version");
        } else if (since != null && !since.isBlank()) {
            try {
                OffsetDateTime sinceDateTime = OffsetDateTime.parse(since);
                return orderRepository.findByTraderAndUpdatedAfter(trader, sinceDateTime, pageable);
            } catch (java.time.format.DateTimeParseException e) {
                throw new org.misha.authservice.exception.BadRequestException("since must be an ISO timestamp with an offset");
            }
        } else {
            return orderRepository.findByTrader(trader, pageable);
        }
    }
}

