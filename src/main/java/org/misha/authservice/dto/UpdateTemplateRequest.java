package org.misha.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateTemplateRequest(
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "categoryId is required") java.util.UUID categoryId,
        BigDecimal dailyRentalPrice,
        BigDecimal depositAmount,
        BigDecimal purchasePrice
) {}
