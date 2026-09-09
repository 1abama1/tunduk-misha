package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    public User register(UserRegistrationDTO dto) {
        String email = normalize(dto.getEmail());
        String phone = normalizePhone(dto.getPhone());

        if (email == null && phone == null) {
            throw new AppException("LOGIN_IDENTIFIER_REQUIRED", "Either email or phone must be provided", HttpStatus.BAD_REQUEST);
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new AppException("PASSWORD_REQUIRED", "Password is required", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .fullName(dto.getFullName() == null ? null : dto.getFullName().trim())
                .email(email)
                .phone(phone)
                .role(Role.TRADER)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Unified message: prevents user enumeration and handles the race
            // between existsByEmail/existsByPhone pre-check and the insert
            throw new AppException("CONFLICT", "Account with these credentials already exists", HttpStatus.CONFLICT);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase();
    }

    private String normalizePhone(String value) {
        return value == null || value.isBlank() ? null : value.replaceAll("[\\s()\\-]", "");
    }

    public String issueTokenForUser(User user) {
        String role = user.getRole() != null ? user.getRole().name() : null;
        return jwtUtil.generateAccessToken(String.valueOf(user.getId()), role);
    }

    public RefreshToken createRefreshForUser(User user) {
        String jti = java.util.UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .jti(jti)
                .expiresAt(java.time.OffsetDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
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
