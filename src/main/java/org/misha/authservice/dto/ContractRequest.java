package org.misha.authservice.dto;

import lombok.Data;

@Data
public class ContractRequest {
    private Long clientId;
    private java.util.UUID templateId;
    private Long toolId;
    private String contractNumber;
    private Double price;
}


