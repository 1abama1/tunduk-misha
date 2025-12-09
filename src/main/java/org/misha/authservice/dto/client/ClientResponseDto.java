package org.misha.authservice.dto.client;

import lombok.Builder;
import org.misha.authservice.entity.ClientTag;
import java.util.Set;

@Builder
public record ClientResponseDto(
        Long id,
        String fullName,
        String phone,
        String whatsappPhone,
        String passportNumber,
        String registrationAddress,
        String livingAddress,
        Integer birthYear,
        Set<ClientTag> tags,
        String lastBranch
) {
}

