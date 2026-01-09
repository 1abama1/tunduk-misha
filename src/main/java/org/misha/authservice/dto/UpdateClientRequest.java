package org.misha.authservice.dto;

import org.misha.authservice.entity.Tag;

import java.time.LocalDate;

public record UpdateClientRequest(
        String fullName,
        String phone,
        String whatsappPhone,
        AddressDto registrationAddress,
        AddressDto livingAddress,
        String email,
        LocalDate birthDate,
        String comment,
        PassportDto passport,
        Tag tag
) {}

