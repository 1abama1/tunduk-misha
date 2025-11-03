package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kyc")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kyc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean simple; // true = simple flow; false = full

    // Full KYC fields
    @Column(unique = true)
    private String inn; // taxpayer id provided in full KYC
    private String fullName;
    private LocalDate birthDate;
    private String residentialAddress;
    private String registeredAddress;

    // Consents
    private boolean consentPersonalData;
    private boolean consentPrivacyPolicy;
    private boolean consentUserAgreement;

    // Files
    private String passportFrontPath;
    private String passportBackPath;
    private String selfieWithPassportPath;

    private OffsetDateTime createdAt;

    // AI extraction fields
    @Lob
    private String extractionJson; // arbitrary JSON with parsed fields
    private String extractionStatus; // pending, extracted, failed
    private String aiModel; // optional model name/version
}


