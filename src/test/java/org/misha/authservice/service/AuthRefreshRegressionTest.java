package org.misha.authservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.exception.RefreshReuseException;
import org.misha.authservice.repository.RefreshTokenRepository;
import org.misha.authservice.repository.UserRepository;
import org.misha.authservice.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthRefreshRegressionTest {
    private final AuthService auth = mock(AuthService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final AuthFacade facade = new AuthFacade(auth, users, tokens, jwt, mock(AuthenticationManager.class));
    private final User user = User.builder().id(1L).build();
    private final RefreshToken stored = RefreshToken.builder().user(user).jti("old")
            .expiresAt(OffsetDateTime.now().plusHours(1)).build();

    @BeforeEach void setup() {
        when(jwt.validateRefreshToken("token")).thenReturn("1");
        when(jwt.getJti("token")).thenReturn("old");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(tokens.findByJti("old")).thenReturn(Optional.of(stored));
    }

    @Test void rejectsJtiOwnedByAnotherUserBeforeRevocation() {
        stored.setUser(User.builder().id(2L).build());
        assertUnauthorized();
        verifyNoInteractions(auth);
        verify(tokens, never()).deleteByUser(any());
    }

    @Test void databaseExpiryIsAlsoEnforced() {
        stored.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        assertUnauthorized();
        verifyNoInteractions(auth);
    }

    @Test void missingJtiNeverReachesDatabaseLookup() {
        when(jwt.getJti("token")).thenReturn(null);
        assertUnauthorized();
        verify(tokens, never()).findByJti(any());
    }

    @Test void revokedTokenInvalidatesUserSessions() {
        stored.setRevoked(true);
        assertThrows(RefreshReuseException.class, () -> facade.refresh("token"));
        verify(tokens).deleteByUser(user);
        verifyNoInteractions(auth);
    }

    @Test void validRotationUsesCurrentUserRoleAndNewJti() {
        when(auth.createRefreshForUser(user)).thenReturn(RefreshToken.builder().jti("new").build());
        when(auth.issueTokenForUser(user)).thenReturn("access");
        when(jwt.generateRefreshToken("1", "new")).thenReturn("refresh");
        var result = facade.refresh("token");
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());
        verify(auth).revokeRefresh(stored);
    }

    private void assertUnauthorized() {
        assertEquals(401, assertThrows(AppException.class, () -> facade.refresh("token")).getStatus().value());
    }
}
