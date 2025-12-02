package org.misha.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientImageDto {
    private Long id;
    private String fileName;
    private String fileType;
}

