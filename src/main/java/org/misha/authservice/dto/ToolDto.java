package org.misha.authservice.dto;

import lombok.Builder;
import lombok.Data;
import org.misha.authservice.entity.ToolStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ToolDto {
    private Long id;
    private String name;
    private String article;
    private String inventoryNumber;
    private ToolStatus status;
    private Long categoryId;
    private String categoryName;
    private Long rentalPointId;
    private String rentalPointName;
    private String description;
    private Double purchasePrice;
    private LocalDate purchaseDate;
    private Double deposit;
    private List<ToolAttributeDto> attributes;
    private List<ToolImageDto> images;
    private LocalDateTime createdAt;
    
    // Для обратной совместимости
    private String serialNumber;
    private ToolTemplateDto template;
}

