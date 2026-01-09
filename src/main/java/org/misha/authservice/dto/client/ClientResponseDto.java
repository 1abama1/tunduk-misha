package org.misha.authservice.dto.client;

import lombok.Builder;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.entity.ClientTag;
import java.util.Set;

@Builder
public record ClientResponseDto(
        Long id,
        String fullName,
        String phone,
        String email,
        String whatsappPhone,
        String passportNumber,
        java.time.LocalDate passportIssuedAt,
        java.time.LocalDate birthDate,
        AddressDto registrationAddress,
        AddressDto livingAddress,
        String comment,
        Integer birthYear,
        Set<ClientTag> tags,
        String lastBranch
) {
}

