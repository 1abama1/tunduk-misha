package org.misha.authservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.TraderRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyOwnershipTest {
    private final TraderRepository repository = mock(TraderRepository.class);
    private final ApiKeyValidationService service = new ApiKeyValidationService(repository);

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void malformedAuthenticatedPrincipalCannotBypassOwnership() {
        authenticate("non-numeric");
        setupTrader(true);
        assertEquals(403, assertThrows(AppException.class, () -> service.validateApiKey("key")).getStatus().value());
    }

    @Test void mismatchedOwnerIsForbidden() {
        authenticate("2");
        setupTrader(true);
        assertThrows(AppException.class, () -> service.validateApiKey("key"));
    }

    @Test void ownerCanUseApprovedKey() {
        authenticate("1");
        var trader = setupTrader(true);
        assertSame(trader, service.validateApiKey("key"));
    }

    @Test void nullApprovalFailsClosed() {
        authenticate("1");
        setupTrader(null);
        assertEquals(403, assertThrows(AppException.class, () -> service.validateApiKey("key")).getStatus().value());
    }

    private Trader setupTrader(Boolean approved) {
        var trader = Trader.builder().id(5L).user(User.builder().id(1L).build()).approved(approved).build();
        when(repository.findByApiKey("key")).thenReturn(Optional.of(trader));
        return trader;
    }

    private void authenticate(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
