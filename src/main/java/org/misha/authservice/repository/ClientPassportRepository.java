package org.misha.authservice.repository;

import org.misha.authservice.entity.ClientPassport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientPassportRepository extends JpaRepository<ClientPassport, Long> {

    Optional<ClientPassport> findByClientId(Long clientId);
}

