package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.AuthResponse;
import org.misha.authservice.dto.LoginRequest;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.security.AuthHeaderBuilder;
import org.misha.authservice.service.AuthFacade;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthFacade authFacade;
    private final AuthHeaderBuilder authHeaderBuilder;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody UserRegistrationDTO dto) {
        AuthResponse response = authFacade.register(dto);
        return buildResponse(response, resolveUid(dto.getEmail(), dto.getPhone()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest login) {
        AuthResponse response = authFacade.login(login);
        return buildResponse(response, resolveUid(login.getEmail(), login.getPhone()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthResponse response = authFacade.refresh(refreshToken);
        return buildResponse(response, null);
    }

    @GetMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshGet(@RequestParam(name = "refreshToken") String refreshToken) {
        AuthResponse response = authFacade.refresh(refreshToken);
        return buildResponse(response, null);
        }

    private ResponseEntity<AuthResponse> buildResponse(AuthResponse response, String uid) {
        HttpHeaders headers = new HttpHeaders();
        authHeaderBuilder.apply(headers, generateOpaqueAccessToken(), uid, generateClientId());
        return ResponseEntity.ok().headers(headers).body(response);
    }

    private String resolveUid(String email, String phone) {
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        return null;
    }

    private String generateOpaqueAccessToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateClientId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
