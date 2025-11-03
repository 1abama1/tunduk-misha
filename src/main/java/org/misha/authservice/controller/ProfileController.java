package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.User;
import org.misha.authservice.entity.Kyc;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.repository.KycRepository;
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
    private final KycRepository kycRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        String subject = authentication.getName();
        // Support both userId (new) and INN (legacy)
        User user = null;
        Long userId = Long.valueOf(subject);
        user = userRepository.findById(userId).orElse(null);
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
        dto.put("passportFrontPath", user.getPassportFrontPath());
        dto.put("passportBackPath", user.getPassportBackPath());
        dto.put("selfieWithPassportPath", user.getSelfieWithPassportPath());

        // Include last KYC (if any)
        var kycs = kycRepository.findByUserOrderByCreatedAtDesc(user);
        if (!kycs.isEmpty()) {
            Kyc last = kycs.get(0);
            Map<String, Object> lastKyc = new LinkedHashMap<>();
            lastKyc.put("id", last.getId());
            lastKyc.put("simple", last.isSimple());
            lastKyc.put("fullName", last.getFullName());
            lastKyc.put("birthDate", last.getBirthDate());
            lastKyc.put("residentialAddress", last.getResidentialAddress());
            lastKyc.put("registeredAddress", last.getRegisteredAddress());
            lastKyc.put("passportFrontPath", last.getPassportFrontPath());
            lastKyc.put("passportBackPath", last.getPassportBackPath());
            lastKyc.put("selfieWithPassportPath", last.getSelfieWithPassportPath());
            lastKyc.put("createdAt", last.getCreatedAt());
            lastKyc.put("extractionStatus", last.getExtractionStatus());
            lastKyc.put("extractionJson", last.getExtractionJson());
            dto.put("lastKyc", lastKyc);
        }
        return ResponseEntity.ok(dto);
    }
}


