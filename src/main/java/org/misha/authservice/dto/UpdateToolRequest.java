package org.misha.authservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateToolRequest(
        String name,
        Long categoryId,
        String description,
        BigDecimal deposit,
        BigDecimal purchasePrice,
        LocalDate purchaseDate
) {}

