package org.misha.authservice.dto;

public record ToolFullDto(
        Long id,
        String name,
        String inventoryNumber,
        String article,
        Double deposit,
        Double purchasePrice,
        Double dailyPrice,
        String status
) {}

