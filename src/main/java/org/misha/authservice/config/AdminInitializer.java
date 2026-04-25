package org.misha.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.misha.authservice.entity.Role;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private static final String ADMIN_EMAIL = "admin@admin.admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password}")
    private String adminPassword;

    public void init() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("Admin already exists");
            return;
        }

        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .fullName("Administrator")
                .role(Role.ADMIN)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build();

        userRepository.save(admin);
        log.info("Admin created (email={})", ADMIN_EMAIL);
    }
}
