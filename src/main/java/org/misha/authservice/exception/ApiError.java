package org.misha.authservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class ApiError {
    private String code;
    private String message;
    private int status;
    private OffsetDateTime timestamp;
}


