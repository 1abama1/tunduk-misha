package org.misha.authservice.dto;

import lombok.Data;
import org.misha.authservice.entity.ToolStatus;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateToolRequest {
    // Обязательные поля для новой архитектуры
    private Long templateId;  // обязательная ссылка на модель
    private String inventoryNumber;  // уникальный инвентарный номер
    private String serialNumber;  // заводской номер
    
    // Дополнительные поля для обратной совместимости
    private String name;
    private String article;
    private ToolStatus status;
    private Long categoryId;
    private Long rentalPointId;
    private String description;
    private Double purchasePrice;
    private LocalDate purchaseDate;
    private Double deposit;
    private List<ToolAttributeRequest> attributes;
    private Long contractId;
    
    @Data
    public static class ToolAttributeRequest {
        private String name;
        private String value;
    }
}