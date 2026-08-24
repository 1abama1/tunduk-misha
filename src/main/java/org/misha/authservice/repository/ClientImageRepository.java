package org.misha.authservice.repository;

import org.misha.authservice.entity.ClientImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientImageRepository extends JpaRepository<ClientImage, Long> {

    List<ClientImage> findByClientId(Long clientId);

    @Query("SELECT i FROM ClientImage i WHERE i.client.id IN :clientIds")
    List<ClientImage> findByClientIdIn(@Param("clientIds") List<Long> clientIds);
}

