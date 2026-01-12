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
        Pageable pageable = PageRequest.of(page, size);

        if (version != null) {
            return orderRepository.findByTraderAndVersionGreaterThan(trader, version, pageable);
        } else if (since != null && !since.isBlank()) {
            try {
                OffsetDateTime sinceDateTime = OffsetDateTime.parse(since);
                return orderRepository.findByTraderAndUpdatedAfter(trader, sinceDateTime, pageable);
            } catch (Exception e) {
                return orderRepository.findByTrader(trader, pageable);
            }
        } else {
            return orderRepository.findByTrader(trader, pageable);
        }
    }
}

