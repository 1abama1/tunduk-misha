package org.misha.authservice.security;

import java.util.Locale;

/** Registration and password authentication must resolve the same stored identifier. */
public final class LoginIdentifiers {
    private LoginIdentifiers() { }

    public static String email(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String phone(String value) {
        return value == null || value.isBlank() ? null : value.replaceAll("[\\s()\\-]", "");
    }
}
