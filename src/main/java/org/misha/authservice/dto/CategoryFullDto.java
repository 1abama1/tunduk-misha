package org.misha.authservice.dto;

import java.util.List;

public record CategoryFullDto(
        java.util.UUID id,
        String name,
        List<TemplateFullDto> templates
) {}

