package org.misha.authservice.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;    // new option
    private String phone;    // new option
    private String password;
}
