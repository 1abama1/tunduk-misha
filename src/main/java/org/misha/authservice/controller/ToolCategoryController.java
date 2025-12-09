package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CategoryDto;
import org.misha.authservice.dto.CategoryFullDto;
import org.misha.authservice.dto.CreateCategoryRequest;
import org.misha.authservice.service.ToolCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class ToolCategoryController {

    private final ToolCategoryService categoryService;

    @PostMapping
    public CategoryDto create(@RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping
    public List<CategoryDto> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}/full")
    public CategoryFullDto getFull(@PathVariable Long id) {
        return categoryService.getFull(id);
    }
}