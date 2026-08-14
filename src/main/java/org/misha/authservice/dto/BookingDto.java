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
        return new BookingDto(
                b.getId(),
                b.getClientName(),
                b.getClientPhone(),
                b.getTemplate().getId(),
                b.getTemplate().getName(),
                b.getToolInstance().getId(),
                b.getToolInstance().getInstanceNumber(),
                b.getStartDateTime(),
                b.getEndDateTime(),
                b.getStatus().name(),
                b.getComment(),
                b.getCreatedAt()
        );
    }
}
