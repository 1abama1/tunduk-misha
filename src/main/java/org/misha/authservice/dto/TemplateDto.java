package org.misha.authservice.dto;

public record TemplateDto(
        java.util.UUID id,
        String name,
        java.util.UUID categoryId
) {}

