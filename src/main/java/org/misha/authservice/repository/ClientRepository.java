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

    List<Client> findByFullNameContainingIgnoreCase(String name);

    List<Client> findByPhone(String phone);

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

    @Query("""
            SELECT DISTINCT c
            FROM Client c
            LEFT JOIN c.documents d
            WHERE (:query IS NULL OR :query = ''
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.registrationAddress) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.livingAddress) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')))
            AND (:tag IS NULL OR c.tag = :tag)
            AND (:hasDocuments IS NULL 
                OR (:hasDocuments = true AND EXISTS (SELECT 1 FROM RentalDocument rd WHERE rd.client.id = c.id))
                OR (:hasDocuments = false AND NOT EXISTS (SELECT 1 FROM RentalDocument rd WHERE rd.client.id = c.id)))
            AND (:contractNumber IS NULL OR :contractNumber = ''
                OR EXISTS (SELECT 1 FROM RentalDocument rd WHERE rd.client.id = c.id 
                    AND LOWER(rd.contractNumber) LIKE LOWER(CONCAT('%', :contractNumber, '%'))))
            AND (:minDocs IS NULL OR (SELECT COUNT(rd) FROM RentalDocument rd WHERE rd.client.id = c.id) >= :minDocs)
            AND (:maxDocs IS NULL OR (SELECT COUNT(rd) FROM RentalDocument rd WHERE rd.client.id = c.id) <= :maxDocs)
            """)
    List<Client> searchAdvanced(
            @Param("query") String query,
            @Param("tag") org.misha.authservice.entity.Tag tag,
            @Param("hasDocuments") Boolean hasDocuments,
            @Param("contractNumber") String contractNumber,
            @Param("minDocs") Integer minDocs,
            @Param("maxDocs") Integer maxDocs
    );
}

