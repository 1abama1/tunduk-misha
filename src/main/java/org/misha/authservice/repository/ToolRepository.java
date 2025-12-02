package org.misha.authservice.repository;

import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ToolRepository extends JpaRepository<Tool, Long> {
    List<Tool> findByContractId(Long contractId);

    @Query(
            value = """
        SELECT * FROM tools 
        WHERE created_at::date = CURRENT_DATE
        """,
            nativeQuery = true
    )
    List<Tool> findCreatedToday();

    List<Tool> findByTemplateId(Long templateId);

    long countByTemplateId(Long templateId);

    long countByTemplateIdAndContractNotNull(Long templateId);

    List<Tool> findByTemplateIdAndContractIsNull(Long templateId);
    
    // Новые методы для расширенной функциональности
    List<Tool> findByStatus(ToolStatus status);
    
    List<Tool> findByCategoryId(Long categoryId);
    
    List<Tool> findByRentalPointId(Long rentalPointId);
    
    @Query("SELECT t FROM Tool t WHERE t.contract IS NULL AND t.status = :status")
    List<Tool> findAvailableByStatus(ToolStatus status);
    
    @Query("SELECT t FROM Tool t WHERE t.contract IS NULL AND (:categoryId IS NULL OR t.category.id = :categoryId) AND (:rentalPointId IS NULL OR t.rentalPoint.id = :rentalPointId)")
    List<Tool> findAvailableTools(Long categoryId, Long rentalPointId);
    
    boolean existsByInventoryNumber(String inventoryNumber);

    @Query("""
        SELECT t
        FROM Tool t
        JOIN FETCH t.template temp
        JOIN FETCH temp.category
        LEFT JOIN FETCH t.contract
    """)
    List<Tool> findAllWithTemplate();

    @Query("""
        SELECT t FROM Tool t
        JOIN FETCH t.template temp
        JOIN FETCH temp.category cat
        LEFT JOIN FETCH t.contract c
        WHERE (:status IS NULL OR t.status = :status)
        AND (:categoryId IS NULL OR cat.id = :categoryId)
    """)
    List<Tool> findFiltered(
        @Param("status") ToolStatus status,
        @Param("categoryId") Long categoryId
    );

    @Query("""
        SELECT t FROM Tool t
        JOIN FETCH t.template temp
        LEFT JOIN FETCH temp.category
        LEFT JOIN FETCH t.contract
        WHERE t.id = :id
    """)
    Optional<Tool> findByIdWithTemplateAndContract(@Param("id") Long id);
}