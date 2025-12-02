package org.misha.authservice.dto;

import org.misha.authservice.entity.Tag;

import java.time.LocalDate;

public record CreateClientRequest(
        String fullName,
        String phone,
        String address,
        String email,
        LocalDate birthDate,
        String comment,
        PassportDto passport,
        Tag tag
) {}

