package org.misha.authservice.dto;

import org.misha.authservice.entity.Tag;

import java.time.LocalDate;

public record UpdateClientRequest(
                String fullName,
                String whatsappPhone,
                String additionalPhone,
                AddressDto registrationAddress,
                AddressDto livingAddress,
                String objectAddress,
                LocalDate birthDate,
                String comment,
                PassportDto passport,
                Tag tag) {
}
