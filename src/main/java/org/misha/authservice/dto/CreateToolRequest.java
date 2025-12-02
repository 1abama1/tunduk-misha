package org.misha.authservice.dto;

import lombok.Data;
import org.misha.authservice.entity.ToolStatus;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateToolRequest {
    private String name;
    private String article;
    private String inventoryNumber;
    private ToolStatus status;
    private Long categoryId;
    private Long rentalPointId;
    private String description;
    private Double purchasePrice;
    private LocalDate purchaseDate;
    private Double deposit;
    private List<ToolAttributeRequest> attributes;
    
    // Для обратной совместимости
    private Long templateId;
    private String serialNumber;
    private Long contractId;
    
    @Data
    public static class ToolAttributeRequest {
        private String name;
        private String value;
    }
}