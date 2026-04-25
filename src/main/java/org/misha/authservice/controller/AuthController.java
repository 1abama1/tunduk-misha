package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.misha.authservice.dto.AuthResponse;
import org.misha.authservice.dto.LoginRequest;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.service.AuthFacade;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthFacade authFacade;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationDTO dto) {
        AuthResponse response = authFacade.register(dto);
        return buildResponse(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest login) {
        AuthResponse response = authFacade.login(login);
        return buildResponse(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthResponse response = authFacade.refresh(refreshToken);
        return buildResponse(response);
    }

    @GetMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshGet(@RequestParam(name = "refreshToken") String refreshToken) {
        AuthResponse response = authFacade.refresh(refreshToken);
        return buildResponse(response);
        }

    private ResponseEntity<AuthResponse> buildResponse(AuthResponse response) {
        HttpHeaders headers = new HttpHeaders();
        // Убрали Opaque Token, токены отдаются просто в JSON-body (AuthResponse)
        return ResponseEntity.ok().headers(headers).body(response);
    }

    // Removed resolveUid method

    // Removed unneeded generating methods
}
