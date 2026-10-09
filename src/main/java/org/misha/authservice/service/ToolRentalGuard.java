package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolRentalGuard {

    private final org.misha.authservice.repository.RentalDocumentRepository documentRepository;
    private final org.misha.authservice.repository.ToolBookingRepository bookingRepository;

    public void ensureNotRented(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            throw new AppException(
                    "TOOL_IS_RENTED",
                    "Инструмент занят действующим договором",
                    HttpStatus.BAD_REQUEST);
        }
    }

    public void ensureNotRented(ToolInstance ToolInstance, String message) {
        if (isRented(ToolInstance)) {
            throw new AppException("TOOL_IS_RENTED", message, HttpStatus.BAD_REQUEST);
        }
    }

    public void ensureAvailableForRental(ToolInstance ToolInstance) {
        if (bookingRepository.findByToolInstanceId(ToolInstance.getId()).stream().anyMatch(b ->
            b.getStatus() == org.misha.authservice.entity.BookingStatus.ACTIVE && b.getEndDateTime().isAfter(java.time.LocalDateTime.now())))
            throw new AppException("TOOL_BOOKED", "Сначала отмените или завершите бронь экземпляра", HttpStatus.CONFLICT);
        if (ToolInstance.getStatus() != org.misha.authservice.entity.ToolInstanceStatus.AVAILABLE)
            throw new AppException("TOOL_NOT_AVAILABLE", "Инструмент недоступен для выдачи", HttpStatus.CONFLICT);
        if (isRented(ToolInstance)) {
            throw new AppException("TOOL_ALREADY_RENTED", "Инструмент занят действующим договором", HttpStatus.CONFLICT);
        }
    }

    public void ensureCanDelete(ToolInstance ToolInstance) {
        if (documentRepository.hasToolHistory(ToolInstance.getId()))
            throw new AppException("TOOL_HAS_HISTORY", "Инструмент связан с историей договоров. Измените его статус вместо удаления.", HttpStatus.CONFLICT);
        if (isRented(ToolInstance)) {
            throw new AppException("CANNOT_DELETE_RENTED_TOOL", "Инструмент занят действующим договором", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isRented(ToolInstance ToolInstance) {
        if (ToolInstance.getContract() != null) {
            if (ToolInstance.getContract().getReturnDate() == null
                    && ToolInstance.getContract().getTerminatedAt() == null) {
                return true;
            }
        }
        return documentRepository.existsByToolIdAndReturnDateIsNullAndTerminatedAtIsNull(ToolInstance.getId());
    }
    
    public String resolveStatus(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            return "RENTED";
        }
        return ToolInstance.getStatus() != null ? ToolInstance.getStatus().name() : "AVAILABLE";
    }
}

