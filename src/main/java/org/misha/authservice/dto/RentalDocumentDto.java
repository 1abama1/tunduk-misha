package org.misha.authservice.dto;

import org.misha.authservice.entity.ContractStatus;

import java.time.LocalDateTime;

public record RentalDocumentDto(
                Long id,
                String contractNumber,
                LocalDateTime startDateTime,
                Double dailyPrice, // Цена за день аренды
                Double amount, // Общая сумма (totalPrice)
                LocalDateTime createdAt,
                Long clientId,
                LocalDateTime returnDate,
                LocalDateTime terminatedAt,
                String terminationReason,
                ContractStatus status,
                String comment) {
}
