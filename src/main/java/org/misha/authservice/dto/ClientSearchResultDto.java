package org.misha.authservice.dto;

public record ClientSearchResultDto(
        Long id,
        String fullName,
        String phone,
        String registrationAddress,
        String livingAddress,
        String tag,
        int documentsCount
) {
}
