package org.misha.authservice.dto;

public record CreateTemplateRequest(
        Long categoryId,
        String name
) {}

