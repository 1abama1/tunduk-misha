package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateContractRequest(
        @NotNull(message = "clientId is required") Long clientId,
        Long toolId,
        List<Long> toolIds,
        String contractNumber,
        String offlineId
) {
}
