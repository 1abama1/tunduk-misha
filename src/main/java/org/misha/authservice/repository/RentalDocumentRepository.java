package org.misha.authservice.repository;

import org.misha.authservice.entity.RentalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RentalDocumentRepository extends JpaRepository<RentalDocument, Long> {

  Optional<RentalDocument> findByOfflineId(String offlineId);

  List<RentalDocument> findByClientId(Long clientId);

  @Query("SELECT d FROM RentalDocument d WHERE d.client.id = :clientId AND d.returnDate IS NULL AND d.terminatedAt IS NULL")
  List<RentalDocument> findActiveContractsByClientId(@Param("clientId") Long clientId);

  List<RentalDocument> findByContractNumberContainingIgnoreCase(String contract);

  boolean existsByContractNumber(String contractNumber);

  @Query("SELECT DISTINCT d FROM RentalDocument d LEFT JOIN FETCH d.tools t LEFT JOIN FETCH t.template tmpl LEFT JOIN FETCH tmpl.category")
  List<RentalDocument> findAllWithTools();

  @Query("SELECT DISTINCT d FROM RentalDocument d LEFT JOIN FETCH d.tools t LEFT JOIN FETCH t.template tmpl LEFT JOIN FETCH tmpl.category WHERE d.id = :id")
  Optional<RentalDocument> findByIdWithTools(@Param("id") Long id);

  @Query("SELECT DISTINCT d FROM RentalDocument d LEFT JOIN FETCH d.tools t LEFT JOIN FETCH t.template tmpl LEFT JOIN FETCH tmpl.category WHERE d.id IN :ids")
  List<RentalDocument> findByIdsWithTools(@Param("ids") List<Long> ids);

  @Query("""
      SELECT COUNT(d)
      FROM RentalDocument d
      WHERE d.createdAt >= :start
        AND d.createdAt < :end
      """)
  long countCreatedBetween(@Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  /**
   * Находит все активные договоры (не закрытые и не расторгнутые).
   * Включает как ACTIVE, так и OVERDUE статусы.
   */
  @Query("""
      SELECT DISTINCT d FROM RentalDocument d
      LEFT JOIN FETCH d.client
      WHERE d.returnDate IS NULL AND d.terminatedAt IS NULL
      ORDER BY d.startDateTime DESC
      """)
  List<RentalDocument> findAllActive();

  boolean existsByToolIdAndReturnDateIsNullAndTerminatedAtIsNull(Long toolId);

  List<RentalDocument> findByToolIdOrderByStartDateTimeDesc(Long toolId);

  @Query("""
      SELECT DISTINCT d FROM RentalDocument d
      LEFT JOIN FETCH d.client c
      LEFT JOIN FETCH d.tools t
      LEFT JOIN FETCH t.template tmpl
      LEFT JOIN FETCH tmpl.category
      WHERE (:clientId IS NULL OR c.id = :clientId)
        AND (:toolId IS NULL OR t.id = :toolId OR d.toolId = :toolId)
      ORDER BY d.startDateTime DESC
      """)
  List<RentalDocument> findHistoryWithoutDate(
      @Param("clientId") Long clientId,
      @Param("toolId") Long toolId);

  @Query("""
      SELECT DISTINCT d FROM RentalDocument d
      LEFT JOIN FETCH d.client c
      LEFT JOIN FETCH d.tools t
      LEFT JOIN FETCH t.template tmpl
      LEFT JOIN FETCH tmpl.category
      WHERE (:clientId IS NULL OR c.id = :clientId)
        AND (:toolId IS NULL OR t.id = :toolId OR d.toolId = :toolId)
        AND d.startDateTime >= :fromDate
      ORDER BY d.startDateTime DESC
      """)
  List<RentalDocument> findHistoryFrom(
      @Param("clientId") Long clientId,
      @Param("toolId") Long toolId,
      @Param("fromDate") LocalDateTime fromDate);

  @Query("""
      SELECT DISTINCT d FROM RentalDocument d
      LEFT JOIN FETCH d.client c
      LEFT JOIN FETCH d.tools t
      LEFT JOIN FETCH t.template tmpl
      LEFT JOIN FETCH tmpl.category
      WHERE (:clientId IS NULL OR c.id = :clientId)
        AND (:toolId IS NULL OR t.id = :toolId OR d.toolId = :toolId)
        AND d.startDateTime < :toDate
      ORDER BY d.startDateTime DESC
      """)
  List<RentalDocument> findHistoryTo(
      @Param("clientId") Long clientId,
      @Param("toolId") Long toolId,
      @Param("toDate") LocalDateTime toDate);

  @Query("""
      SELECT DISTINCT d FROM RentalDocument d
      LEFT JOIN FETCH d.client c
      LEFT JOIN FETCH d.tools t
      LEFT JOIN FETCH t.template tmpl
      LEFT JOIN FETCH tmpl.category
      WHERE (:clientId IS NULL OR c.id = :clientId)
        AND (:toolId IS NULL OR t.id = :toolId OR d.toolId = :toolId)
        AND d.startDateTime >= :fromDate
        AND d.startDateTime < :toDate
      ORDER BY d.startDateTime DESC
      """)
  List<RentalDocument> findHistoryBetween(
      @Param("clientId") Long clientId,
      @Param("toolId") Long toolId,
      @Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate);
}
