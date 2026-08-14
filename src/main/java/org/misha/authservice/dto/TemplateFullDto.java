package org.misha.authservice.dto;

import java.util.List;

public record TemplateFullDto(
        java.util.UUID id,
        String name,
        java.util.UUID categoryId,
        java.math.BigDecimal dailyRentalPrice,
        java.math.BigDecimal depositAmount,
        java.math.BigDecimal purchasePrice,
        List<ToolDtoSimple> tools
) {}

