package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        String subject = authentication.getName();
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid user identifier"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", user.getId());
        dto.put("fullName", user.getFullName());
        dto.put("birthDate", user.getBirthDate());
        dto.put("residentialAddress", user.getAddressLiving());
        dto.put("registeredAddress", user.getAddressRegistration());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone());
        dto.put("consentPersonalData", user.isConsentPersonalData());
        dto.put("consentPrivacyPolicy", user.isConsentPrivacyPolicy());
        dto.put("consentUserAgreement", user.isConsentUserAgreement());
        dto.put("simpleMode", user.isSimpleMode());
        return ResponseEntity.ok(dto);
    }
}
