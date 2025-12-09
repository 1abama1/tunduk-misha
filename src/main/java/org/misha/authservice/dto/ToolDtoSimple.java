package org.misha.authservice.dto;

import org.misha.authservice.entity.Tool;

public record ToolDtoSimple(
        Long id,
        String name,
        String inventoryNumber,
        String article,
        Double deposit,
        Double purchasePrice,
        Double dailyPrice,
        String status
) {
    public static ToolDtoSimple fromEntity(Tool t) {
        return new ToolDtoSimple(
                t.getId(),
                t.getName(),
                t.getInventoryNumber(),
                t.getArticle(),
                t.getDeposit(),
                t.getPurchasePrice(),
                t.getDailyPrice(),
                t.getStatus().name()
        );
    }
}

