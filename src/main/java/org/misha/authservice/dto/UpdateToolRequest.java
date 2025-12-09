package org.misha.authservice.dto;

import java.math.BigDecimal;

public record UpdateToolRequest(
        String name,
        Long categoryId,
        BigDecimal deposit,
        BigDecimal purchasePrice,
        BigDecimal dailyPrice
) {}

