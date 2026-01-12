package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Product;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ProductSyncService {
    private final ProductRepository productRepository;

    public Page<Product> syncProducts(Trader trader, String since, Long version, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (version != null) {
            return productRepository.findByTraderAndVersionGreaterThan(trader, version, pageable);
        } else if (since != null && !since.isBlank()) {
            try {
                OffsetDateTime sinceDateTime = OffsetDateTime.parse(since);
                return productRepository.findByTraderAndUpdatedAfter(trader, sinceDateTime, pageable);
            } catch (Exception e) {
                return productRepository.findByTrader(trader, pageable);
            }
        } else {
            return productRepository.findByTrader(trader, pageable);
        }
    }
}

