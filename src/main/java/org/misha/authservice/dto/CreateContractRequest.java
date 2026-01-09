package org.misha.authservice.dto;

import java.time.LocalDateTime;

public record CreateContractRequest(
        Long clientId,
        Long toolId,
        String contractNumber,
        LocalDateTime startDateTime
) {
}

