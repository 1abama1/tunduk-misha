package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;
import org.misha.authservice.entity.ToolStatus;

public record UpdateToolStatusRequest(
        @NotNull(message = "status is required") ToolStatus status,
        /** Необязательная причина смены статуса (например, причина отправки в ремонт) */
        String reason
) {}

