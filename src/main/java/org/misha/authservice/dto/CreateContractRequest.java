package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;

public record CreateContractRequest(
        @NotNull(message = "clientId is required") Long clientId,
        @NotNull(message = "toolId is required") Long toolId,
        String contractNumber,
        java.time.LocalDateTime startDateTime
) {
}
