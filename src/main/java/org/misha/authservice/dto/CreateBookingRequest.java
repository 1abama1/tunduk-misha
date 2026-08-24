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
        @NotNull(message = "hours is required") 
        @jakarta.validation.constraints.Min(value = 1, message = "Minimum 1 hour") 
        @jakarta.validation.constraints.Max(value = 6, message = "Maximum 6 hours") 
        Integer hours,
        String comment
) {}
