package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ADMIN;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_tags", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag")
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();
}

