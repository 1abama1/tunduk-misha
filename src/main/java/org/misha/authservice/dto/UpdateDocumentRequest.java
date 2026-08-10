package org.misha.authservice.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UpdateDocumentRequest {
    private String contractNumber;
    private LocalDateTime startDateTime;
    private LocalDate expectedReturnDate;
    private Double amount;
    private java.util.UUID categoryId;
    private Long toolId;
}

