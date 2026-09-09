package org.misha.authservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveContractRowDto {
    private Long id;             // contract id
    private Long clientId;       // id клиента — нужен фронту для навигации
    private String clientName;
    private String toolName;
    private String startDate;
    private Double balance;
    private Double dailyPrice;
}

