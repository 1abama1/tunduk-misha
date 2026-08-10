package org.misha.authservice.dto;

import org.misha.authservice.entity.ToolBooking;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingDto(
        UUID id,
        Long clientId,
        String clientName,
        UUID templateId,
        String templateName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String status,
        String comment,
        LocalDateTime createdAt
) {
    public static BookingDto fromEntity(ToolBooking b) {
        return new BookingDto(
                b.getId(),
                b.getClient().getId(),
                b.getClient().getFullName(),
                b.getTemplate().getId(),
                b.getTemplate().getName(),
                b.getStartDateTime(),
                b.getEndDateTime(),
                b.getStatus().name(),
                b.getComment(),
                b.getCreatedAt()
        );
    }
}
