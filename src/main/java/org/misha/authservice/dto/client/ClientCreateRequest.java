package org.misha.authservice.dto.client;

import org.misha.authservice.dto.AddressDto;
import java.time.LocalDate;

public record ClientCreateRequest(
        String fullName,
        String phone,
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

