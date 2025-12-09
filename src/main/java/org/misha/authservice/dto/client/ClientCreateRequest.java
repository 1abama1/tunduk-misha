package org.misha.authservice.dto.client;

import java.time.LocalDate;

public record ClientCreateRequest(
        String fullName,
        String phone,
        String whatsappPhone,
        String registrationAddress,
        String livingAddress,
        String passportNumber,
        LocalDate passportIssuedAt,
        String pin,
        Integer birthYear,
        String email,
        String comment
) {
}

