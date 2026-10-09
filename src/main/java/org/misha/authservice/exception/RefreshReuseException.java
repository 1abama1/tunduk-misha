package org.misha.authservice.exception;
public class RefreshReuseException extends AppException {
    public RefreshReuseException() { super("REFRESH_TOKEN_REUSE", "Session terminated", org.springframework.http.HttpStatus.UNAUTHORIZED); }
}
