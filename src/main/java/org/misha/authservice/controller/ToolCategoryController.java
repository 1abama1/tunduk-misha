package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolCategoryDto;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.service.ToolCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools/categories")
@RequiredArgsConstructor
public class ToolCategoryController {
    private final ToolCategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<ToolCategoryDto>> getAllCategories() {
        List<ToolCategory> categories = categoryService.getAllCategories();
        List<ToolCategoryDto> dtos = categories.stream()
                .map(cat -> new ToolCategoryDto(cat.getId(), cat.getName()))
                .toList();
        return ResponseEntity.ok(dtos);
    }
}