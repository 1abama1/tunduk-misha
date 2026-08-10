package org.misha.authservice.dto;

import lombok.Data;

@Data
public class ToolTemplateDto {
    private java.util.UUID id;
    private String name;
    private java.util.UUID categoryId;
    private String categoryName;
}
