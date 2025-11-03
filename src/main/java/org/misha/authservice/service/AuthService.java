package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.repository.RefreshTokenRepository;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public User register(UserRegistrationDTO dto) {
        if ((dto.getEmail() == null || dto.getEmail().isBlank()) && (dto.getPhone() == null || dto.getPhone().isBlank())) {
            throw new IllegalArgumentException("Either email or phone must be provided");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank() && userRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("Phone already in use");
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public String login(String inn, String email, String phone, String password) {
        User user = null;
        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null && phone != null && !phone.isBlank()) {
            user = userRepository.findByPhone(phone).orElse(null);
        }
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Use userId as subject for future-proofing
        return jwtUtil.generateAccessToken(String.valueOf(user.getId()));
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
