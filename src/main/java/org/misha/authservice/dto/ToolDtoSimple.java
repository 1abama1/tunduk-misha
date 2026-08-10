package org.misha.authservice.dto;

import org.misha.authservice.entity.ToolInstance;
import java.util.UUID;
import java.math.BigDecimal;

public record ToolDtoSimple(
        Long id,
        String name,
        String inventoryNumber,
        Integer instanceNumber,
        String article,
        BigDecimal deposit,
        BigDecimal purchasePrice,
        BigDecimal dailyPrice,
        String status
) {
    public static ToolDtoSimple fromEntity(ToolInstance t) {
        return new ToolDtoSimple(
                t.getId(),
                t.getTemplate() != null ? t.getTemplate().getName() : null,
                t.getInventoryNumber(),
                t.getInstanceNumber(),
                null,
                t.getTemplate() != null ? t.getTemplate().getDepositAmount() : null,
                null,
                t.getTemplate() != null ? t.getTemplate().getDailyRentalPrice() : null,
                t.getContract() != null ? "RENTED" : (t.getStatus() != null ? t.getStatus().name() : "AVAILABLE")
        );
    }
}
