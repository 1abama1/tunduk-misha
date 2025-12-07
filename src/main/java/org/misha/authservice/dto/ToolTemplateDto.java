package org.misha.authservice.dto;

import lombok.Data;

@Data
public class ToolTemplateDto {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
}
