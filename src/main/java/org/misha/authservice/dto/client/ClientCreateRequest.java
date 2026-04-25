package org.misha.authservice.dto.client;

import jakarta.validation.constraints.NotBlank;
import org.misha.authservice.dto.AddressDto;
import java.time.LocalDate;

public record ClientCreateRequest(
        @NotBlank(message = "fullName is required") String fullName,
        @NotBlank(message = "phone is required") String phone,
        String whatsappPhone,
        AddressDto registrationAddress,
        AddressDto livingAddress,
        String passportNumber,
        LocalDate passportIssuedAt,
        LocalDate birthDate,
        String pin,
        Integer birthYear,
        String email,
        String comment
) {
}
