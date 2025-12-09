package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Double purchasePrice;
    private Double deposit;
    private Double dailyPrice;
    private List<ToolAttributeDto> attributes;
    private List<ToolImageDto> images;
    private LocalDateTime createdAt;
    
    // Для обратной совместимости
    private String serialNumber;
    private ToolTemplateDto template;

    public static ToolDto fromEntity(Tool tool) {
        ToolDto dto = new ToolDto();
        dto.setId(tool.getId());
        dto.setName(tool.getName());
        dto.setArticle(tool.getArticle());
        dto.setInventoryNumber(tool.getInventoryNumber());
        dto.setStatus(tool.getStatus());
        dto.setPurchasePrice(tool.getPurchasePrice());
        dto.setDeposit(tool.getDeposit());
        dto.setDailyPrice(tool.getDailyPrice());
        dto.setCreatedAt(tool.getCreatedAt());
        dto.setSerialNumber(tool.getSerialNumber());

        // Категория берется из template
        if (tool.getTemplate() != null && tool.getTemplate().getCategory() != null) {
            dto.setCategoryId(tool.getTemplate().getCategory().getId());
            dto.setCategoryName(tool.getTemplate().getCategory().getName());
        }

        if (tool.getRentalPoint() != null) {
            dto.setRentalPointId(tool.getRentalPoint().getId());
            dto.setRentalPointName(tool.getRentalPoint().getName());
        }

        if (tool.getTemplate() != null) {
            ToolTemplateDto templateDto = new ToolTemplateDto();
            templateDto.setId(tool.getTemplate().getId());
            templateDto.setName(tool.getTemplate().getName());
            dto.setTemplate(templateDto);
        }

        // Атрибуты
        if (tool.getAttributes() != null && !tool.getAttributes().isEmpty()) {
            dto.setAttributes(tool.getAttributes().stream()
                    .map(attr -> new ToolAttributeDto(attr.getId(), attr.getName(), attr.getValue()))
                    .collect(Collectors.toList()));
        } else {
            dto.setAttributes(new ArrayList<>());
        }

        // Изображения
        if (tool.getImages() != null && !tool.getImages().isEmpty()) {
            dto.setImages(tool.getImages().stream()
                    .map(img -> new ToolImageDto(img.getId(), img.getFileName(), img.getContentType()))
                    .collect(Collectors.toList()));
        } else {
            dto.setImages(new ArrayList<>());
        }

        return dto;
    }
}

