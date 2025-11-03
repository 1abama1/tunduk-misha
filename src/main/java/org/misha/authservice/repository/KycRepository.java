package org.misha.authservice.repository;

import org.misha.authservice.entity.Kyc;
import org.misha.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycRepository extends JpaRepository<Kyc, Long> {
    List<Kyc> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByInn(String inn);
}


