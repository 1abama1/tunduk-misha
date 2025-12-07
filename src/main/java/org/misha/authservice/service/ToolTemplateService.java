package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolTemplateDto;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolTemplateService {
    private final ToolTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public List<ToolTemplateDto> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolTemplateDto> getTemplatesByCategory(Long categoryId) {
        return templateRepository.findByCategoryId(categoryId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolTemplateDto getTemplateById(Long id) {
        ToolTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new org.misha.authservice.exception.AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found",
                        org.springframework.http.HttpStatus.NOT_FOUND));
        return toDto(template);
    }

    private ToolTemplateDto toDto(ToolTemplate template) {
        ToolTemplateDto dto = new ToolTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        if (template.getCategory() != null) {
            dto.setCategoryId(template.getCategory().getId());
            dto.setCategoryName(template.getCategory().getName());
        }
        return dto;
    }
}