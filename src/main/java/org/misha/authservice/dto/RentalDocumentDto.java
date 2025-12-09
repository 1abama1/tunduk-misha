package org.misha.authservice.dto;

import org.misha.authservice.entity.ContractStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalDocumentDto(
        Long id,
        String contractNumber,
        LocalDateTime startDateTime,
        LocalDate expectedReturnDate,
        Double dailyPrice,      // Цена за день аренды
        Double amount,          // Общая сумма (totalPrice)
        LocalDateTime createdAt,
        Long clientId,
        LocalDateTime closedAt,
        LocalDateTime terminatedAt,
        String terminationReason,
        ContractStatus status,
        String comment
) {}

