package org.misha.authservice.repository;

import org.misha.authservice.entity.Trader;
import org.misha.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TraderRepository extends JpaRepository<Trader, Long> {
    Optional<Trader> findByUser(User user);
    Optional<Trader> findByApiKey(String apiKey);
    Optional<Trader> findByUserId(Long userId);
}

