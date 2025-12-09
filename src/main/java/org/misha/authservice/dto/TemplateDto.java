package org.misha.authservice.dto;

public record TemplateDto(
        Long id,
        String name,
        Long categoryId
) {}

