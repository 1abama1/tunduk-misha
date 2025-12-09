package org.misha.authservice.dto.tool;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ToolHistoryDto(
        Long contractId,
        String clientName,
        LocalDateTime startDate,
        LocalDate expectedReturn,
        LocalDateTime closedAt,
        Double amount,
        String status
) {
}

