package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateTemplateRequest;
import org.misha.authservice.dto.TemplateDto;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToolTemplateService {
    private final ToolTemplateRepository templateRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolInstanceRepository ToolInstanceRepository;

    @Transactional
    public TemplateDto create(CreateTemplateRequest request) {
        ToolCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Category not found"));

        ToolTemplate template = templateRepository.save(
                ToolTemplate.builder()
                        .name(request.name())
                        .category(category)
                        .build()
        );

        return new TemplateDto(template.getId(), template.getName(), category.getId());
    }

    @Transactional(readOnly = true)
    public List<TemplateDto> getByCategory(UUID categoryId) {
        return templateRepository.findByCategoryId(categoryId)
                .stream()
                .map(t -> new TemplateDto(t.getId(), t.getName(), categoryId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateFullDto getFull(UUID id) {
        ToolTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Template not found"));

        List<ToolInstance> tools = ToolInstanceRepository.findByTemplateId(template.getId());

        return new TemplateFullDto(
                template.getId(),
                template.getName(),
                tools.stream().map(ToolDtoSimple::fromEntity).toList()
        );
    }
}
