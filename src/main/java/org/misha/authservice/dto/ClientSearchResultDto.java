package org.misha.authservice.dto;

public record ClientSearchResultDto(
        Long id,
        String fullName,
        String phone,
        String email,
        String address,
        String tag,
        int documentsCount
) {
}

