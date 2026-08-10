package org.misha.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotNull(message = "categoryId is required") java.util.UUID categoryId,
        @NotBlank(message = "name is required") String name
) {}
