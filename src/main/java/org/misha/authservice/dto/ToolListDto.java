package org.misha.authservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ToolListDto(
        Long id,
        String name,
        String inventoryNumber,
        Integer instanceNumber,
        String status,
        String categoryName,
        BigDecimal deposit
) {}
