package org.misha.authservice.controller;

import jakarta.validation.Valid;
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
    public CategoryDto create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping
    public List<CategoryDto> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}/full")
    public CategoryFullDto getFull(@PathVariable java.util.UUID id) {
        return categoryService.getFull(id);
    }

    @GetMapping("/all/full")
    public List<CategoryFullDto> getAllFull() {
        return categoryService.getAllFull();
    }
}