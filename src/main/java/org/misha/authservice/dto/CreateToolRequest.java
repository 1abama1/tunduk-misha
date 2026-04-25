package org.misha.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateToolRequest(
        @NotNull(message = "templateId is required") Long templateId,
        @NotBlank(message = "name is required") String name,
        String inventoryNumber,
        String article,
        @NotNull(message = "deposit is required") Double deposit,
        Double purchasePrice,
        @NotNull(message = "dailyPrice is required") Double dailyPrice
) {}
