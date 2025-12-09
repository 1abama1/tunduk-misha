package org.misha.authservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveContractRowDto {
    private Long id;             // contract id
    private String clientName;
    private String toolName;
    private String startDate;
    private Double balance;
}

