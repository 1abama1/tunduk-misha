package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolInstanceRepository extends JpaRepository<ToolInstance, Long> {
    
    @Query("SELECT MAX(t.inventoryNumber) FROM ToolInstance t WHERE t.template.id = :templateId")
    String findMaxInventoryNumberByTemplateId(@Param("templateId") UUID templateId);

    @Query("SELECT MAX(t.instanceNumber) FROM ToolInstance t WHERE t.template.id = :templateId")
    Integer findMaxInstanceNumberByTemplateId(@Param("templateId") UUID templateId);

    List<ToolInstance> findByTemplateId(UUID templateId);
    
    boolean existsByInventoryNumber(String inventoryNumber);
    
    List<ToolInstance> findByUpdatedAtAfter(LocalDateTime since);

    @Query(value = "SELECT * FROM tool_instances WHERE created_at::date = CURRENT_DATE", nativeQuery = true)
    List<ToolInstance> findCreatedToday();

    @Query("SELECT t FROM ToolInstance t JOIN FETCH t.template temp JOIN FETCH temp.category WHERE t.id = :id")
    Optional<ToolInstance> findByIdWithTemplate(@Param("id") Long id);

    @Query("SELECT t FROM ToolInstance t WHERE t.contract.id = :contractId")
    List<ToolInstance> findByContractId(@Param("contractId") Long contractId);

    long countByTemplateId(UUID templateId);

    @Query("SELECT COUNT(t) FROM ToolInstance t WHERE t.template.id = :templateId AND t.status = :status")
    long countByTemplateIdAndStatus(@Param("templateId") UUID templateId, @Param("status") org.misha.authservice.entity.ToolInstanceStatus status);

    @Query("SELECT COUNT(t) FROM ToolInstance t WHERE t.template.id = :templateId AND t.contract IS NOT NULL AND t.status = 'AVAILABLE'")
    long countByTemplateIdAndContractNotNull(@Param("templateId") UUID templateId);

    @Query("SELECT t FROM ToolInstance t WHERE t.template.id = :templateId AND t.contract IS NULL AND t.status = 'AVAILABLE'")
    List<ToolInstance> findByTemplateIdAndContractIsNull(@Param("templateId") UUID templateId);

    @Query("SELECT COUNT(t) FROM ToolInstance t WHERE t.contract IS NULL AND t.status = 'AVAILABLE'")
    long countByContractIsNull();

    @Query("SELECT COUNT(t) FROM ToolInstance t WHERE t.contract IS NOT NULL AND t.status = 'AVAILABLE'")
    long countByContractNotNull();

    @Query("SELECT t FROM ToolInstance t JOIN FETCH t.template WHERE t.id = :id")
    Optional<ToolInstance> findByIdWithTemplateAndContract(@Param("id") Long id);

    @Query("SELECT t FROM ToolInstance t JOIN FETCH t.template WHERE t.contract.id = :contractId")
    List<ToolInstance> findByContractIdWithTemplate(@Param("contractId") Long contractId);

    @Query("SELECT t FROM ToolInstance t JOIN FETCH t.template temp JOIN FETCH temp.category")
    List<ToolInstance> findAllWithTemplate();

    @Query("SELECT t FROM ToolInstance t WHERE t.template.id = :templateId AND t.status = 'AVAILABLE' AND t.id NOT IN (SELECT rd.toolId FROM RentalDocument rd WHERE rd.toolId = t.id AND rd.returnDate IS NULL AND rd.startDateTime <= :endDate AND (rd.returnDate >= :startDate OR rd.returnDate IS NULL))")
    List<ToolInstance> findAvailableForPeriod(@Param("templateId") UUID templateId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}