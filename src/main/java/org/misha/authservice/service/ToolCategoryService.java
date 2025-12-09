package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CategoryDto;
import org.misha.authservice.dto.CategoryFullDto;
import org.misha.authservice.dto.CreateCategoryRequest;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolCategoryService {
    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolRepository toolRepository;

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        ToolCategory category = categoryRepository.save(
                ToolCategory.builder().name(request.name()).build()
        );
        return new CategoryDto(category.getId(), category.getName());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryFullDto getFull(Long id) {
        ToolCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Category not found"));

        List<ToolTemplate> templates = templateRepository.findByCategoryId(id);

        List<TemplateFullDto> templateDtos = templates.stream().map(t -> {
            List<Tool> tools = toolRepository.findByTemplateId(t.getId());

            List<ToolDtoSimple> toolDtos = tools.stream().map(ToolDtoSimple::fromEntity).toList();

            return new TemplateFullDto(t.getId(), t.getName(), toolDtos);
        }).toList();

        return new CategoryFullDto(category.getId(), category.getName(), templateDtos);
    }
}