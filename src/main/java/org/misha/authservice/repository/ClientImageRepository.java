package org.misha.authservice.repository;

import org.misha.authservice.entity.ClientImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientImageRepository extends JpaRepository<ClientImage, Long> {

    List<ClientImage> findByClientId(Long clientId);
}

