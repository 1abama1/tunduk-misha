package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private LocalDate birthDate; // optional, may be filled by KYC
    private String addressLiving; // from KYC
    private String addressRegistration; // from KYC

    @Column(unique = true)
    private String email;
    @Column(unique = true)
    private String phone;

    private String passwordHash;

    private boolean consentPersonalData;
    private boolean consentPrivacyPolicy;
    private boolean consentUserAgreement;

    private boolean simpleMode;

    // KYC uploaded file paths
    private String passportFrontPath;
    private String passportBackPath;
    private String selfieWithPassportPath;
}

