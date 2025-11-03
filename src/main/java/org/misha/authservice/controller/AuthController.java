package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.LoginRequest;
import org.misha.authservice.dto.UserRegistrationDTO;
import org.misha.authservice.entity.User;
import org.misha.authservice.service.AuthService;
import org.misha.authservice.security.JwtUtil;
import org.misha.authservice.entity.Kyc;
import org.misha.authservice.repository.KycRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final KycRepository kycRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto) {
        User saved;
        try {
            saved = authService.register(dto);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
        String token = authService.issueTokenForUser(saved);
        var refreshEntity = authService.createRefreshForUser(saved);
        String refreshJwt = jwtUtil.generateRefreshToken(String.valueOf(saved.getId()), refreshEntity.getJti());
        String principal = dto.getEmail() != null && !dto.getEmail().isBlank() ? dto.getEmail() : dto.getPhone();
        if (principal != null && dto.getPassword() != null) {
            UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(principal, dto.getPassword());
            Authentication auth = authenticationManager.authenticate(authReq);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        String clientId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String opaqueAccessToken = UUID.randomUUID().toString().replace("-", "");
        String uid = dto.getEmail() != null ? dto.getEmail() : (dto.getPhone() != null ? dto.getPhone() : "");
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + opaqueAccessToken)
                .header("Access-Token", opaqueAccessToken)
                .header("Token-Type", "Bearer")
                .header("Uid", uid)
                .header("Client", clientId)
                .header("Cache-Control", "max-age=0, private, must-revalidate")
                .header("Access-Control-Expose-Headers", "Authorization,Access-Token,Token-Type,Uid,Client,Cache-Control,Content-Type")
                .body(Map.of(
                        "userId", saved.getId(),
                        "accessToken", token,
                        "refreshToken", refreshJwt
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest login) {
        String principalLogin = login.getEmail() != null && !login.getEmail().isBlank() ? login.getEmail() : login.getPhone();
        UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(principalLogin, login.getPassword());
        Authentication auth = authenticationManager.authenticate(authReq);
        SecurityContextHolder.getContext().setAuthentication(auth);
        String token = authService.login(null, login.getEmail(), login.getPhone(), login.getPassword());
        var userId = jwtUtil.parse(token).getBody().getSubject();
        var user = userRepository.findById(Long.valueOf(userId)).orElseThrow();
        var refreshEntity = authService.createRefreshForUser(user);
        String refreshJwt = jwtUtil.generateRefreshToken(userId, refreshEntity.getJti());
        String clientId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String opaqueAccessToken = UUID.randomUUID().toString().replace("-", "");
        String uid = login.getEmail() != null ? login.getEmail() : (login.getPhone() != null ? login.getPhone() : "");
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + opaqueAccessToken)
                .header("Access-Token", opaqueAccessToken)
                .header("Token-Type", "Bearer")
                .header("Uid", uid)
                .header("Client", clientId)
                .header("Cache-Control", "max-age=0, private, must-revalidate")
                .header("Access-Control-Expose-Headers", "Authorization,Access-Token,Token-Type,Uid,Client,Cache-Control,Content-Type")
                .body(Map.of(
                        "accessToken", token,
                        "refreshToken", refreshJwt
                ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken is required"));
        }
        try {
            String subject = jwtUtil.validateRefreshToken(refreshToken);
            String oldJti = jwtUtil.getJti(refreshToken);
            var user = userRepository.findById(Long.valueOf(subject)).orElseThrow();
            var oldEntityOpt = refreshTokenRepository.findByJti(oldJti);
            if (oldEntityOpt.isPresent() && !oldEntityOpt.get().isRevoked()) {
                authService.revokeRefresh(oldEntityOpt.get());
            } else {
                refreshTokenRepository.deleteByUser(user);
            }
            String newAccess = jwtUtil.generateAccessToken(subject);
            var newEntity = authService.createRefreshForUser(user);
            String newRefresh = jwtUtil.generateRefreshToken(subject, newEntity.getJti());
            String clientId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String opaqueAccessToken = UUID.randomUUID().toString().replace("-", "");
            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + opaqueAccessToken)
                    .header("Access-Token", opaqueAccessToken)
                    .header("Token-Type", "Bearer")
                    .header("Client", clientId)
                    .header("Cache-Control", "max-age=0, private, must-revalidate")
                    .header("Access-Control-Expose-Headers", "Authorization,Access-Token,Token-Type,Uid,Client,Cache-Control,Content-Type")
                    .body(Map.of(
                            "accessToken", newAccess,
                            "refreshToken", newRefresh
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh token"));
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshGet(@RequestParam(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken is required"));
        }
        try {
            String subject = jwtUtil.validateRefreshToken(refreshToken);
            String oldJti = jwtUtil.getJti(refreshToken);
            var user = userRepository.findById(Long.valueOf(subject)).orElseThrow();
            var oldEntityOpt = refreshTokenRepository.findByJti(oldJti);
            if (oldEntityOpt.isPresent() && !oldEntityOpt.get().isRevoked()) {
                authService.revokeRefresh(oldEntityOpt.get());
            } else {
                refreshTokenRepository.deleteByUser(user);
            }
            String newAccess = jwtUtil.generateAccessToken(subject);
            var newEntity = authService.createRefreshForUser(user);
            String newRefresh = jwtUtil.generateRefreshToken(subject, newEntity.getJti());
            String clientId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String opaqueAccessToken = UUID.randomUUID().toString().replace("-", "");
            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + opaqueAccessToken)
                    .header("Access-Token", opaqueAccessToken)
                    .header("Token-Type", "Bearer")
                    .header("Client", clientId)
                    .header("Cache-Control", "max-age=0, private, must-revalidate")
                    .header("Access-Control-Expose-Headers", "Authorization,Access-Token,Token-Type,Uid,Client,Cache-Control,Content-Type")
                    .body(Map.of(
                            "accessToken", newAccess,
                            "refreshToken", newRefresh
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid refresh token"));
        }
    }

    @PostMapping(value = "/kyc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitKyc(
            Authentication authentication,
            @RequestParam("inn") String inn,
            @RequestParam("fullName") String fullName,
            @RequestParam("birthDate") String birthDate,
            @RequestParam("residentialAddress") String residentialAddress,
            @RequestParam("registeredAddress") String registeredAddress,
            @RequestParam("consentPersonalData") boolean consentPersonalData,
            @RequestParam("consentPrivacyPolicy") boolean consentPrivacyPolicy,
            @RequestParam("consentUserAgreement") boolean consentUserAgreement,
            @RequestParam("passportFront") MultipartFile passportFront,
            @RequestParam("passportBack") MultipartFile passportBack,
            @RequestParam("selfieWithPassport") MultipartFile selfieWithPassport
    ) throws IOException {
        validateConsents(consentPersonalData, consentPrivacyPolicy, consentUserAgreement);
        if (inn == null || !inn.matches("^\\d{14}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "inn must be exactly 14 digits"));
        }
        if (kycRepository.existsByInn(inn)) {
            return ResponseEntity.status(409).body(Map.of("error", "inn already used"));
        }

        Long userId = Long.valueOf(authentication.getName());
        var user = userRepository.findById(userId).orElseThrow();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String folderName = sanitize(userId + "_" + timestamp);
        Path baseDir = Paths.get("uploads", "kyc", folderName);
        Files.createDirectories(baseDir);

        Map<String, String> savedFiles = new HashMap<>();
        String passportFrontPath = saveFile(baseDir, "passport_front_", passportFront);
        String passportBackPath = saveFile(baseDir, "passport_back_", passportBack);
        String selfiePath = saveFile(baseDir, "selfie_with_passport_", selfieWithPassport);
        savedFiles.put("passportFront", passportFrontPath);
        savedFiles.put("passportBack", passportBackPath);
        savedFiles.put("selfieWithPassport", selfiePath);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inn", inn);
        payload.put("fullName", fullName);
        payload.put("birthDate", birthDate);
        payload.put("residentialAddress", residentialAddress);
        payload.put("registeredAddress", registeredAddress);
        payload.put("consentPersonalData", consentPersonalData);
        payload.put("consentPrivacyPolicy", consentPrivacyPolicy);
        payload.put("consentUserAgreement", consentUserAgreement);
        payload.put("files", savedFiles);
        payload.put("folder", baseDir.toString());

        Kyc kyc = Kyc.builder()
                .user(user)
                .simple(false)
                .inn(inn)
                .fullName(fullName)
                .birthDate(LocalDate.parse(birthDate))
                .residentialAddress(residentialAddress)
                .registeredAddress(registeredAddress)
                .consentPersonalData(consentPersonalData)
                .consentPrivacyPolicy(consentPrivacyPolicy)
                .consentUserAgreement(consentUserAgreement)
                .passportFrontPath(passportFrontPath)
                .passportBackPath(passportBackPath)
                .selfieWithPassportPath(selfiePath)
                .createdAt(java.time.OffsetDateTime.now())
                .extractionStatus("pending")
                .build();
        kycRepository.save(kyc);

        return ResponseEntity.ok(payload);
    }

    @PostMapping(value = "/kyc/simple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitKycSimple(
            Authentication authentication,
            @RequestParam("consentPersonalData") boolean consentPersonalData,
            @RequestParam("consentPrivacyPolicy") boolean consentPrivacyPolicy,
            @RequestParam("consentUserAgreement") boolean consentUserAgreement,
            @RequestParam("selfieWithPassport") MultipartFile selfieWithPassport,
            @RequestParam(value = "passportFront", required = false) MultipartFile passportFront,
            @RequestParam(value = "passportBack", required = false) MultipartFile passportBack
    ) throws IOException {
        validateConsents(consentPersonalData, consentPrivacyPolicy, consentUserAgreement);
        Long userId = Long.valueOf(authentication.getName());
        var user = userRepository.findById(userId).orElseThrow();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String folderName = "simple_" + userId + "_" + timestamp;
        Path baseDir = Paths.get("uploads", "kyc", sanitize(folderName));
        Files.createDirectories(baseDir);

        Map<String, String> savedFiles = new HashMap<>();
        savedFiles.put("selfieWithPassport", saveFile(baseDir, "selfie_with_passport_", selfieWithPassport));
        if (passportFront != null && !passportFront.isEmpty()) {
            savedFiles.put("passportFront", saveFile(baseDir, "passport_front_", passportFront));
        }
        if (passportBack != null && !passportBack.isEmpty()) {
            savedFiles.put("passportBack", saveFile(baseDir, "passport_back_", passportBack));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("consentPersonalData", consentPersonalData);
        payload.put("consentPrivacyPolicy", consentPrivacyPolicy);
        payload.put("consentUserAgreement", consentUserAgreement);
        payload.put("files", savedFiles);
        payload.put("folder", baseDir.toString());

        Kyc kyc = Kyc.builder()
                .user(user)
                .simple(true)
                .consentPersonalData(consentPersonalData)
                .consentPrivacyPolicy(consentPrivacyPolicy)
                .consentUserAgreement(consentUserAgreement)
                .passportFrontPath(savedFiles.get("passportFront"))
                .passportBackPath(savedFiles.get("passportBack"))
                .selfieWithPassportPath(savedFiles.get("selfieWithPassport"))
                .createdAt(java.time.OffsetDateTime.now())
                .extractionStatus("pending")
                .build();
        kycRepository.save(kyc);

        return ResponseEntity.ok(payload);
    }

    private String saveFile(Path baseDir, String prefix, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File " + prefix + " is missing");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            extension = original.substring(dot);
        }
        String filename = sanitize(prefix + System.nanoTime() + extension);
        Path target = baseDir.resolve(filename);
        Files.copy(file.getInputStream(), target);
        return target.toString();
    }

    private void validateConsents(boolean consentPersonalData, boolean consentPrivacyPolicy, boolean consentUserAgreement) {
        if (!consentPersonalData || !consentPrivacyPolicy || !consentUserAgreement) {
            throw new IllegalArgumentException("All consents must be accepted");
        }
    }

    private String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
