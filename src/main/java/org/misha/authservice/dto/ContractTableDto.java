package org.misha.authservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.misha.authservice.entity.ContractStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractTableDto {
    private Long id;
    private String contractNumber;

    private Long clientId;        // id клиента — нужен фронту для навигации и кеширования
    private String clientName;
    private String toolName;
    private String serialNumber;

    private LocalDateTime startDateTime;
    private LocalDateTime returnDate;

    private Double amount;
    private ContractStatus status;
}
