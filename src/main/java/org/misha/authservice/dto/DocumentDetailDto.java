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
public class DocumentDetailDto {
    private Long id;
    private String contractNumber;
    private Double amount;
    private Double dailyPrice;
    private LocalDateTime startDateTime;
    private LocalDateTime createdAt;
    private LocalDateTime returnDate;
    private LocalDateTime terminatedAt;
    private String terminationReason;
    private ContractStatus status;
    private String comment;

    private Long clientId;
    private ClientDto client;

    private Long toolId;
    private ToolDto tool;
}
