package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateToolStatusRequest(
        @NotNull(message = "status is required") String status,
        String reason
) {}
