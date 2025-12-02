package org.misha.authservice.dto;

import lombok.Data;

@Data
public class CreateDocumentRequest {
    private Long clientId;
    private String contractNumber;
    private Long categoryId;
    private Long toolId; // ID ФИЗИЧЕСКОГО инструмента
}
