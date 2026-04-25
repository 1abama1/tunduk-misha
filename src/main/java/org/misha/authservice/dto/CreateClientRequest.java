package org.misha.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.misha.authservice.entity.Tag;

import java.time.LocalDate;

public record CreateClientRequest(
                @NotBlank(message = "fullName is required") String fullName,
                @NotBlank(message = "phone is required") String phone,
                String whatsappPhone,
                AddressDto registrationAddress,
                AddressDto livingAddress,
                String objectAddress,
                String email,
                LocalDate birthDate,
                String comment,
                PassportDto passport,
                Tag tag) {
}
