package org.misha.authservice.dto;

public record CreateToolRequest(
        Long templateId,
        String name,
        String inventoryNumber,
        String article,
        Double deposit,
        Double purchasePrice,
        Double dailyPrice
) {}