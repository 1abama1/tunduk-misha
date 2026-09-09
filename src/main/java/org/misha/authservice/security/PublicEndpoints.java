package org.misha.authservice.security;

/**
 * Single source of truth for public (unauthenticated) endpoints.
 * Used by both SecurityConfig and JwtFilter to keep them in sync (DRY).
 */
public final class PublicEndpoints {
    private PublicEndpoints() {
    }

    public static final String[] PATHS = {
            "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
            "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh"
    };
}
