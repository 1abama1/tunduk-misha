package org.misha.authservice.dto;

import java.math.BigDecimal;

public record UpdateToolRequest(
        String name,
        java.util.UUID categoryId,
        BigDecimal deposit,
        BigDecimal purchasePrice,
        BigDecimal dailyPrice
) {}

