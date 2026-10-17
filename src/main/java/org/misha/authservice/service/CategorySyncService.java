package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategorySyncService {
    private final ToolCategoryRepository categoryRepository;

    public Page<ToolCategory> syncCategories(String since, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), org.springframework.data.domain.Sort.by("id"));
        return categoryRepository.findAll(pageable);
    }
}

