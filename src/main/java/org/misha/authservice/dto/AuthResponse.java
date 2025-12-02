package org.misha.authservice.dto;

public record AuthResponse(
        Long userId,
        String accessToken,
        String refreshToken
) {
}

