package org.misha.authservice.dto;

import lombok.Data;

@Data
public class ContractRequest {
    private Long clientId;
    private Long templateId;
    private Long toolId;
    private String contractNumber;
    private Double price;
}


