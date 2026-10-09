package org.misha.authservice.repository;

import org.misha.authservice.entity.RefreshToken;
import org.misha.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByJti(String jti);
    void deleteByUser(User user);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") OffsetDateTime now);
}


