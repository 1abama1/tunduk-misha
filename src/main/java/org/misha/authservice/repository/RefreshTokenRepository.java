package org.misha.authservice.repository;

import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByJti(String jti);
    void deleteByUser(User user);
}


