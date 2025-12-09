package org.misha.authservice.dto;

import java.time.LocalDate;
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

    private String clientName;
    private String toolName;
    private String serialNumber;

    private LocalDateTime startDateTime;
    private LocalDate expectedReturnDate;
    private LocalDateTime actualReturnDate;

    private Double amount;
    private ContractStatus status;
}

