package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveContractDto {
    private Long id;
    private String contractNumber;
    private String toolName;
    private String serialNumber;
    private LocalDateTime startDateTime;
}

