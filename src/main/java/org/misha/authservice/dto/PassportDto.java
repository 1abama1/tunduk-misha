package org.misha.authservice.dto;

import java.time.LocalDate;

public record PassportDto(
        String series,
        String number,
        String issuedBy,
        String subdivisionCode,
        LocalDate issueDate
) {
}

