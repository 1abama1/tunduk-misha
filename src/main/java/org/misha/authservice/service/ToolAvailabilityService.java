package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolAvailabilityService {

    private final ToolRepository toolRepository;
    private final ToolTemplateRepository templateRepository;

    public int getAvailableCount(Long templateId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));

        long total = toolRepository.countByTemplateId(templateId);
        long rented = toolRepository.countByTemplateIdAndContractNotNull(templateId);

        return (int) (total - rented);
    }

    public boolean isAvailable(Long templateId) {
        return getAvailableCount(templateId) > 0;
    }

    public List<Tool> getAvailableTools(Long templateId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));
        return toolRepository.findByTemplateIdAndContractIsNull(templateId);
    }
}

