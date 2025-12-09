package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateTemplateRequest;
import org.misha.authservice.dto.TemplateDto;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.service.ToolTemplateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ToolTemplateController {

    private final ToolTemplateService service;

    @PostMapping
    public TemplateDto create(@RequestBody CreateTemplateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TemplateDto> getByCategory(@RequestParam Long categoryId) {
        return service.getByCategory(categoryId);
    }

    @GetMapping("/{id}")
    public TemplateFullDto getOne(@PathVariable Long id) {
        return service.getFull(id);
    }
}