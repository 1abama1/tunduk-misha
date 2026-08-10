package org.misha.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.misha.authservice.entity.Tag;

import java.time.LocalDate;

public record CreateClientRequest(
                @NotBlank(message = "fullName is required") String fullName,
                @NotBlank(message = "whatsappPhone is required") String whatsappPhone,
                String additionalPhone,
                AddressDto registrationAddress,
                AddressDto livingAddress,
                String objectAddress,
                LocalDate birthDate,
                String comment,
                PassportDto passport,
                Tag tag) {
}
