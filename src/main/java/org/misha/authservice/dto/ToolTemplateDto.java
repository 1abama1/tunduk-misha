package org.misha.authservice.dto;

import lombok.Data;

@Data
public class ToolTemplateDto {
    private Long id;
    private String name;
    private String categoryName;
    private String description;
    private Boolean available;
    private Integer totalCount;
    private Integer availableCount;
}
