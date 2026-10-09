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
                String comment,
                String offlineId,
                Long toolId,
                java.util.Set<Long> toolIds,
                LocalDateTime updatedAt) {
    public RentalDocumentDto(Long id, String contractNumber, LocalDateTime startDateTime, Double dailyPrice,
        Double amount, LocalDateTime createdAt, Long clientId, LocalDateTime returnDate, LocalDateTime terminatedAt,
        String terminationReason, ContractStatus status, String comment, String offlineId) {
        this(id, contractNumber, startDateTime, dailyPrice, amount, createdAt, clientId, returnDate, terminatedAt,
            terminationReason, status, comment, offlineId, null, java.util.Set.of(), null);
    }
    public static RentalDocumentDto fromEntity(org.misha.authservice.entity.RentalDocument d) {
        var ids = new java.util.LinkedHashSet<>(d.getHistoricalToolIds());
        if (d.getToolId() != null) ids.add(d.getToolId());
        return new RentalDocumentDto(d.getId(), d.getContractNumber(), d.getStartDateTime(), d.getDailyPrice(),
          d.getAmount(), d.getCreatedAt(), d.getClient() == null ? null : d.getClient().getId(), d.getReturnDate(),
          d.getTerminatedAt(), d.getTerminationReason(), d.getStatus(), d.getComment(), d.getOfflineId(),
          d.getToolId(), ids, d.getUpdatedAt());
    }
}
