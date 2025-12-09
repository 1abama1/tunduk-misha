package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;
    private String fullName;
    private String phone;
    private String whatsappPhone;
    private String address;
    private String email;
    private LocalDate birthDate;
    private String comment;
    private PassportDto passport;
    private String tag;
    private List<DocumentDto> documents;
    private List<ClientImageDto> images;
}

