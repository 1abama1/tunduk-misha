package org.misha.authservice.dto;

import lombok.Builder;
import lombok.Data;
import org.misha.authservice.entity.ContractStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentDto {
    private Long id;
    private String contractNumber;
    private String category;
    private String toolName;
    private String serialNumber;
    private LocalDateTime startDateTime;
    private LocalDate expectedReturnDate;
    private Double amount;
    private Long toolId;
    private LocalDateTime closedAt;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private ContractStatus status;
}

