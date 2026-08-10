package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull(message = "client ID is required") Long clientId,
        @NotNull(message = "template ID is required") UUID templateId,
        @NotNull(message = "start date time is required") LocalDateTime startDateTime,
        @NotNull(message = "end date time is required") LocalDateTime endDateTime,
        String comment
) {}
