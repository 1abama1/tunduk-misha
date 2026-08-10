package org.misha.authservice.repository;

import org.misha.authservice.entity.ToolBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Repository
public interface ToolBookingRepository extends JpaRepository<ToolBooking, UUID> {

    @Query("SELECT COUNT(b) FROM ToolBooking b WHERE b.template.id = :templateId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDateTime < :endDate " +
           "AND b.endDateTime > :startDate")
    int countActiveBookingsByTemplateAndDates(
            @Param("templateId") UUID templateId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
            
    List<ToolBooking> findByTemplateId(UUID templateId);
}
