package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateBookingRequest(
        @jakarta.validation.constraints.NotBlank(message = "client name is required") String clientName,
        String clientPhone,
        @NotNull(message = "template ID is required") UUID templateId,
        @NotNull(message = "tool instance ID is required") Long toolInstanceId,
        @NotNull(message = "start date time is required") LocalDateTime startDateTime,
        @NotNull(message = "end date time is required") LocalDateTime endDateTime,
        String comment
) {}
