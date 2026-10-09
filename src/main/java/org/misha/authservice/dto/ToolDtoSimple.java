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
        String status,
        UUID activeBookingId,
        UUID templateId
) {
    @com.fasterxml.jackson.annotation.JsonProperty("dailyRentalPrice")
    public BigDecimal dailyRentalPrice() { return dailyPrice; }
    @com.fasterxml.jackson.annotation.JsonProperty("depositAmount")
    public BigDecimal depositAmount() { return deposit; }
    public static ToolDtoSimple fromEntity(ToolInstance t) {
        return fromEntity(t, null);
    }

    public static ToolDtoSimple fromEntity(ToolInstance t, UUID activeBookingId) {
        return new ToolDtoSimple(
                t.getId(),
                t.getTemplate() != null ? t.getTemplate().getName() : null,
                t.getInventoryNumber(),
                t.getInstanceNumber(),
                null,
                t.getTemplate() != null ? t.getTemplate().getDepositAmount() : null,
                t.getTemplate() != null ? t.getTemplate().getPurchasePrice() : null,
                t.getTemplate() != null ? t.getTemplate().getDailyRentalPrice() : null,
                t.getContract() != null ? "RENTED" : (activeBookingId != null ? "BOOKED" : (t.getStatus() != null ? t.getStatus().name() : "AVAILABLE")),
                activeBookingId,
                t.getTemplate() != null ? t.getTemplate().getId() : null
        );
    }
}
