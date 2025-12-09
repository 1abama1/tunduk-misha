package org.misha.authservice.dto;

import java.util.List;

public record CategoryFullDto(
        Long id,
        String name,
        List<TemplateFullDto> templates
) {}

