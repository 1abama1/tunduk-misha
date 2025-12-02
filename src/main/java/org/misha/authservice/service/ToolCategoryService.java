package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolCategoryService {
    private final ToolCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ToolCategory> getAllCategories() {
        return categoryRepository.findAll();
    }
}