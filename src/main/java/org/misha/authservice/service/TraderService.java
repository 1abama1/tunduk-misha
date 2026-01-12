package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Trader;
import org.misha.authservice.entity.User;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.TraderRepository;
import org.misha.authservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TraderService {
    private final TraderRepository traderRepository;
    private final UserRepository userRepository;

    public String generateApiKey(Long traderId) {
        Trader trader = traderRepository.findById(traderId)
                .orElseThrow(() -> new AppException("TRADER_NOT_FOUND", "Trader not found", HttpStatus.NOT_FOUND));

        if (!trader.getApproved()) {
            throw new AppException("TRADER_NOT_APPROVED", "Trader must be approved before generating API key", HttpStatus.BAD_REQUEST);
        }

        String apiKey = UUID.randomUUID().toString().replace("-", "");
        trader.setApiKey(apiKey);
        traderRepository.save(trader);

        return apiKey;
    }

    public String getApiKey(Long traderId) {
        Trader trader = traderRepository.findById(traderId)
                .orElseThrow(() -> new AppException("TRADER_NOT_FOUND", "Trader not found", HttpStatus.NOT_FOUND));

        if (trader.getApiKey() == null || trader.getApiKey().isBlank()) {
            throw new AppException("API_KEY_NOT_GENERATED", "API key has not been generated yet", HttpStatus.NOT_FOUND);
        }

        return trader.getApiKey();
    }

    @Transactional
    public Trader createTraderForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        if (traderRepository.findByUser(user).isPresent()) {
            throw new AppException("TRADER_ALREADY_EXISTS", "Trader already exists for this user", HttpStatus.CONFLICT);
        }

        Trader trader = Trader.builder()
                .user(user)
                .approved(false)
                .build();

        return traderRepository.save(trader);
    }
}

