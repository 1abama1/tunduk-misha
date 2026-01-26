package org.misha.authservice.dto.tool;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ToolHistoryDto(
                Long contractId,
                String clientName,
                LocalDateTime startDate,
                LocalDateTime returnDate,
                Double amount,
                String status) {
}
