package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CategorySyncResponse;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.service.CategorySyncService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
public class AdminCategorySyncController {
    private final CategorySyncService categorySyncService;

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TRADER')")
    public ResponseEntity<CategorySyncResponse> syncCategories(
            @RequestParam(required = false) String since,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ToolCategory> categoryPage = categorySyncService.syncCategories(since, page, size);

        CategorySyncResponse response = CategorySyncResponse.builder()
                .content(categoryPage.getContent())
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .hasNext(categoryPage.hasNext())
                .hasPrevious(categoryPage.hasPrevious())
                .build();

        return ResponseEntity.ok(response);
    }
}

