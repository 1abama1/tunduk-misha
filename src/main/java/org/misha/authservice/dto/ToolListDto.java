package org.misha.authservice.dto;

import org.misha.authservice.entity.ToolStatus;

import java.math.BigDecimal;

public record ToolListDto(
        Long id,
        String name,
        String inventoryNumber,
        ToolStatus status,
        String categoryName,
        BigDecimal deposit
) {}

