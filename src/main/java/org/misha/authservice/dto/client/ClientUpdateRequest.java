package org.misha.authservice.dto.client;

import org.misha.authservice.dto.AddressDto;
import java.time.LocalDate;

public record ClientUpdateRequest(
        String fullName,
        String whatsappPhone,
        String additionalPhone,
        AddressDto registrationAddress,
        AddressDto livingAddress,
        String passportNumber,
        LocalDate passportIssuedAt,
        LocalDate birthDate,
        String pin,
        Integer birthYear,
        String comment
) {
}
