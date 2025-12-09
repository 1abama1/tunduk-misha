package org.misha.authservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RentRequest(
        @NotNull(message = "Tool ID is required")
        Long toolId,
        
        @NotNull(message = "Client ID is required")
        Long clientId,
        
        @NotNull(message = "Rent days is required")
        @Positive(message = "Rent days must be positive")
        Integer rentDays,
        
        @NotNull(message = "Price per day is required")
        @Positive(message = "Price per day must be positive")
        Double pricePerDay
) {
}

