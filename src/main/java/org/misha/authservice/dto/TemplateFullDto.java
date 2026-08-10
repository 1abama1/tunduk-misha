package org.misha.authservice.dto;

import java.util.List;

public record TemplateFullDto(
        java.util.UUID id,
        String name,
        List<ToolDtoSimple> tools
) {}

