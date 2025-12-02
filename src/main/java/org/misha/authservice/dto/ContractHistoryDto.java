package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.misha.authservice.entity.ContractStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractHistoryDto {
    private Long id;
    private String contractNumber;
    private String toolName;
    private LocalDateTime startDateTime;
    private LocalDateTime closedAt;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private ContractStatus status;
}

