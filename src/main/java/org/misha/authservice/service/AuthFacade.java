package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.AuthResponse;
import org.misha.authservice.dto.LoginRequest;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(UserRegistrationDTO dto) {
        User saved = authService.register(dto);
        authenticatePrincipal(dto.getEmail(), dto.getPhone(), dto.getPassword());
        return issueTokens(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticatePrincipal(request.getEmail(), request.getPhone(), request.getPassword());
        if (authentication == null) {
            throw new AppException("INVALID_CREDENTIALS", "Credentials are required", HttpStatus.BAD_REQUEST);
        }
        Long userId = Long.valueOf(authentication.getPrincipal().toString());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "User not found", HttpStatus.UNAUTHORIZED));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException("REFRESH_TOKEN_REQUIRED", "refreshToken is required", HttpStatus.BAD_REQUEST);
        }
        try {
            String subject = jwtUtil.validateRefreshToken(refreshToken);
            String oldJti = jwtUtil.getJti(refreshToken);
            User user = userRepository.findById(Long.valueOf(subject))
                    .orElseThrow(() -> new AppException("USER_NOT_FOUND", "User not found", HttpStatus.UNAUTHORIZED));

            refreshTokenRepository.findByJti(oldJti)
                    .ifPresentOrElse(token -> {
                        if (!token.isRevoked()) {
                            authService.revokeRefresh(token);
                        } else {
                            refreshTokenRepository.deleteByUser(user);
                        }
                    }, () -> refreshTokenRepository.deleteByUser(user));

            RefreshToken newEntity = authService.createRefreshForUser(user);
            String newRefresh = jwtUtil.generateRefreshToken(subject, newEntity.getJti());
            String access = jwtUtil.generateAccessToken(subject);
            return new AuthResponse(user.getId(), access, newRefresh);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception e) {
            throw new AppException("INVALID_REFRESH_TOKEN", "Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = authService.issueTokenForUser(user);
        RefreshToken refreshToken = authService.createRefreshForUser(user);
        String refreshJwt = jwtUtil.generateRefreshToken(String.valueOf(user.getId()), refreshToken.getJti());
        return new AuthResponse(user.getId(), accessToken, refreshJwt);
    }

    private Authentication authenticatePrincipal(String email, String phone, String password) {
        String principal = resolvePrincipal(email, phone);
        if (!StringUtils.hasText(principal) || !StringUtils.hasText(password)) {
            return null;
        }
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(principal, password);
        Authentication authentication = authenticationManager.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return authentication;
    }

    private String resolvePrincipal(String email, String phone) {
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        return null;
    }
}

