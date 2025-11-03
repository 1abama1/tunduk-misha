package org.misha.authservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRegistrationDTO {
    private String fullName; // optional
    private String email;    // one of email/phone required
    private String phone;    // one of email/phone required
    private String password; // required
}