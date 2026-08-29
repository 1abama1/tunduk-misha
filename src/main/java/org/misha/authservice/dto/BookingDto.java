package org.misha.authservice.dto;

import org.misha.authservice.entity.ToolBooking;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingDto(
        UUID id,
        String clientName,
        String clientPhone,
        UUID templateId,
        String templateName,
        Long toolInstanceId,
        Integer toolInstanceNumber,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String status,
        String comment,
        LocalDateTime createdAt
) {
    public static BookingDto fromEntity(ToolBooking b) {
        UUID templateId = null;
        String templateName = null;
        if (b.getTemplate() != null) {
            try {
                templateId = b.getTemplate().getId();
                templateName = b.getTemplate().getName();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                // Ignore soft-deleted entity proxy initialization failure
            }
        }

        Long toolInstanceId = null;
        Integer toolInstanceNumber = null;
        if (b.getToolInstance() != null) {
            try {
                toolInstanceId = b.getToolInstance().getId();
                toolInstanceNumber = b.getToolInstance().getInstanceNumber();
            } catch (jakarta.persistence.EntityNotFoundException e) {
                // Ignore soft-deleted entity proxy initialization failure
            }
        }

        return new BookingDto(
                b.getId(),
                b.getClientName(),
                b.getClientPhone(),
                templateId,
                templateName,
                toolInstanceId,
                toolInstanceNumber,
                b.getStartDateTime(),
                b.getEndDateTime(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getComment(),
                b.getCreatedAt()
        );
    }
}
