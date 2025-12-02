package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public User register(UserRegistrationDTO dto) {
        if ((dto.getEmail() == null || dto.getEmail().isBlank()) && (dto.getPhone() == null || dto.getPhone().isBlank())) {
            throw new AppException("LOGIN_IDENTIFIER_REQUIRED", "Either email or phone must be provided", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new AppException("PASSWORD_REQUIRED", "Password is required", HttpStatus.BAD_REQUEST);
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException("EMAIL_EXISTS", "Email already in use", HttpStatus.CONFLICT);
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank() && userRepository.existsByPhone(dto.getPhone())) {
            throw new AppException("PHONE_EXISTS", "Phone already in use", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public String issueTokenForUser(User user) {
        return jwtUtil.generateAccessToken(String.valueOf(user.getId()));
    }

    public RefreshToken createRefreshForUser(User user) {
        String jti = java.util.UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .jti(jti)
                .expiresAt(java.time.OffsetDateTime.now().plus(Duration.ofMillis(604800000L))) // align with properties
                .revoked(false)
                .createdAt(java.time.OffsetDateTime.now())
                .build();
        return refreshTokenRepository.save(token);
    }

    public void revokeRefresh(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }
}
