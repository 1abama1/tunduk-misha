package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Kyc;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.KycRepository;
import org.misha.authservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {
    private final KycRepository kycRepository;
    private final UserRepository userRepository;

    /**
     * Stub endpoint: marks latest (or specified) KYC as extracted successfully with provided JSON payload
     */
    @PostMapping("/latest/verify")
    public ResponseEntity<?> verifyLatest(Authentication authentication, @RequestBody(required = false) Map<String, Object> payload) {
        Long userId = Long.valueOf(authentication.getName());
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        var kycs = kycRepository.findByUserOrderByCreatedAtDesc(user);
        if (kycs.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "No KYC found"));
        Kyc kyc = kycs      .get(0);
        kyc.setExtractionStatus("extracted");
        if (payload != null) {
            kyc.setExtractionJson(toJson(payload));
        } else {
            kyc.setExtractionJson("{\"note\":\"stub passed\"}");
        }
        kycRepository.save(kyc);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("kycId", kyc.getId());
        res.put("extractionStatus", kyc.getExtractionStatus());
        res.put("extractionJson", kyc.getExtractionJson());
        return ResponseEntity.ok(res);
    }

    private String toJson(Map<String, Object> map) {
        // very naive json builder to avoid adding libs; keys assumed safe
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('\"').append(e.getKey()).append('\"').append(":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append('\"').append(String.valueOf(v).replace("\"", "\\\"")).append('\"');
            }
        }
        sb.append("}");
        return sb.toString();
    }
}


