package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;

public record ReturnRequest(
        @NotNull(message = "Contract ID is required")
        Long contractId,
        /** Если true — инструмент после возврата переводится в IN_REPAIR */
        boolean isBroken
) {
}
