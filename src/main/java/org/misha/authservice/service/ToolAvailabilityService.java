package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolInstanceStatus;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolBookingRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolAvailabilityService {

    private final ToolInstanceRepository toolInstanceRepository;
    private final ToolTemplateRepository templateRepository;
    private final RentalDocumentRepository rentalDocumentRepository;
    private final ToolBookingRepository toolBookingRepository;

    @Transactional(readOnly = true)
    public int getAvailableCount(UUID templateId) {
        validateTemplate(templateId);
        long total = toolInstanceRepository.countByTemplateIdAndStatus(templateId, ToolInstanceStatus.AVAILABLE);
        long rented = toolInstanceRepository.countByTemplateIdAndContractNotNull(templateId);
        return (int) (total - rented);
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(UUID templateId) {
        return getAvailableCount(templateId) > 0;
    }

    @Transactional(readOnly = true)
    public List<ToolInstance> getAvailableTools(UUID templateId) {
        validateTemplate(templateId);
        return toolInstanceRepository.findByTemplateIdAndContractIsNull(templateId);
    }

    @Transactional(readOnly = true)
    public int getAvailableCount(UUID templateId, LocalDateTime startDate, LocalDateTime endDate) {
        validateTemplate(templateId);
        long total = toolInstanceRepository.countByTemplateIdAndStatus(templateId, ToolInstanceStatus.AVAILABLE);
        int busyInRange = rentalDocumentRepository.countBusyToolsByTemplateAndDates(
                templateId, startDate, endDate);
        int bookedInRange = toolBookingRepository.countActiveBookingsByTemplateAndDates(
                templateId, startDate, endDate);
                
        int available = (int) (total - busyInRange - bookedInRange);
        return Math.max(available, 0);
    }

    @Transactional(readOnly = true)
    public boolean isAvailableForPeriod(UUID templateId, LocalDateTime startDate, LocalDateTime endDate) {
        return getAvailableCount(templateId, startDate, endDate) > 0;
    }

    @Transactional(readOnly = true)
    public int getInstanceAvailableCount(Long toolInstanceId, LocalDateTime startDate, LocalDateTime endDate) {
        ToolInstance toolInstance = toolInstanceRepository.findById(toolInstanceId)
                .orElseThrow(() -> new AppException("INSTANCE_NOT_FOUND", "Instance not found: " + toolInstanceId, HttpStatus.NOT_FOUND));

        if (toolInstance.getStatus() != ToolInstanceStatus.AVAILABLE) {
            return 0;
        }

        int busyInRange = rentalDocumentRepository.countBusyToolsByInstanceAndDates(
                toolInstanceId, startDate, endDate);
        int bookedInRange = toolBookingRepository.countActiveBookingsByToolInstanceAndDates(
                toolInstanceId, startDate, endDate);
                
        return (busyInRange == 0 && bookedInRange == 0) ? 1 : 0;
    }

    @Transactional(readOnly = true)
    public boolean isInstanceAvailableForPeriod(Long toolInstanceId, LocalDateTime startDate, LocalDateTime endDate) {
        return getInstanceAvailableCount(toolInstanceId, startDate, endDate) > 0;
    }

    private void validateTemplate(UUID templateId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found: " + templateId,
                        HttpStatus.NOT_FOUND));
    }
}
