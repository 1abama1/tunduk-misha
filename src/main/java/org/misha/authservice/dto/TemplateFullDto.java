package org.misha.authservice.dto;

import java.util.List;

public record TemplateFullDto(
        Long id,
        String name,
        List<ToolDtoSimple> tools
) {}

