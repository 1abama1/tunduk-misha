package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.TraderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class SyncTraderResolver {

    private final ApiKeyValidationService apiKeyValidationService;
    private final TraderRepository traderRepository;

    public Trader resolve(String apiKey, Long traderId) {
        Trader trader = null;
        boolean isAdmin = isAdmin();

        if (apiKey != null && !apiKey.isBlank()) {
            trader = apiKeyValidationService.validateApiKey(apiKey);
        } else {
            if (!isAdmin) {
                throw new AppException("API_KEY_REQUIRED", "X-API-KEY header is required for traders", HttpStatus.UNAUTHORIZED);
            }
            if (traderId != null) {
                trader = traderRepository.findById(traderId)
                        .orElseThrow(() -> new AppException("TRADER_NOT_FOUND", "Trader not found", HttpStatus.NOT_FOUND));
            } else {
                throw new AppException("TRADER_ID_OR_API_KEY_REQUIRED", "Either traderId parameter or X-API-KEY header is required", HttpStatus.BAD_REQUEST);
            }
        }

        if (traderId != null && trader != null && !trader.getId().equals(traderId)) {
            throw new AppException("TRADER_ID_MISMATCH", "Trader ID does not match API key", HttpStatus.FORBIDDEN);
        }

        return trader;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
