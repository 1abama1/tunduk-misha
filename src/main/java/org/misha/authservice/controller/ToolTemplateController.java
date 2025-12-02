package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolTemplateDto;
import org.misha.authservice.service.ToolTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools/templates")
@RequiredArgsConstructor
public class ToolTemplateController {
    private final ToolTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<ToolTemplateDto>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ToolTemplateDto>> getTemplatesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(templateService.getTemplatesByCategory(categoryId));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ToolTemplateDto>> getAvailableTemplates() {
        return ResponseEntity.ok(templateService.getAvailableTemplates());
    }
}