package org.misha.authservice.repository;

import org.misha.authservice.dto.ClientLightSearchDto;
import org.misha.authservice.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    Optional<Client> findByEmail(String email);

    @Query("SELECT DISTINCT c FROM Client c LEFT JOIN FETCH c.documents LEFT JOIN FETCH c.passport")
    List<Client> findAllWithDocuments();

    @Query("SELECT DISTINCT c FROM Client c LEFT JOIN FETCH c.documents LEFT JOIN FETCH c.passport WHERE c.id = :id")
    Optional<Client> findByIdWithDocuments(@Param("id") Long id);

    @Query("""
            SELECT new org.misha.authservice.dto.ClientLightSearchDto(
                c.id,
                c.fullName,
                c.phone,
                c.email,
                CASE WHEN EXISTS (
                    SELECT 1 FROM RentalDocument d
                    WHERE d.client.id = c.id AND d.closedAt IS NULL AND d.terminatedAt IS NULL
                )
                THEN true ELSE false END
            )
            FROM Client c
            WHERE (:query IS NULL
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR c.phone LIKE CONCAT('%', :query, '%')
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY c.fullName ASC
            """)
    List<ClientLightSearchDto> searchLight(@Param("query") String query);
}

