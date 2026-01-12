package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.TraderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiKeyValidationService {
    private final TraderRepository traderRepository;

    public Trader validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException("API_KEY_REQUIRED", "X-API-KEY header is required", HttpStatus.UNAUTHORIZED);
        }

        Trader trader = traderRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new AppException("INVALID_API_KEY", "Invalid API key", HttpStatus.UNAUTHORIZED));

        if (!trader.getApproved()) {
            throw new AppException("TRADER_NOT_APPROVED", "Trader is not approved", HttpStatus.FORBIDDEN);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            try {
                Long userId = Long.valueOf(authentication.getPrincipal().toString());
                if (!trader.getUser().getId().equals(userId)) {
                    throw new AppException("API_KEY_MISMATCH", "API key does not belong to the authenticated user", HttpStatus.FORBIDDEN);
                }
            } catch (NumberFormatException e) {
            }
        }

        return trader;
    }
}

