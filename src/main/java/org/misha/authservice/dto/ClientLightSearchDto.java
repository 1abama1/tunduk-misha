package org.misha.authservice.dto;

public record ClientLightSearchDto(
        Long id,
        String fullName,
        String phone,
        String email,
        boolean hasActiveContract
) {
}

