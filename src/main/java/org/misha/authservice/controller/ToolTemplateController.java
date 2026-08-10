package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateTemplateRequest;
import org.misha.authservice.dto.TemplateDto;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.service.ToolAvailabilityService;
import org.misha.authservice.service.ToolTemplateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class ToolTemplateController {

    private final ToolTemplateService service;
    private final ToolAvailabilityService availabilityService;

    @PostMapping
    public TemplateDto create(@Valid @RequestBody CreateTemplateRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TemplateDto> getByCategory(@RequestParam java.util.UUID categoryId) {
        return service.getByCategory(categoryId);
    }

    @GetMapping("/{id}")
    public TemplateFullDto getOne(@PathVariable java.util.UUID id) {
        return service.getFull(id);
    }

    /**
     * Проверяет доступность шаблона на указанный период.
     *
     * <p>GET /api/templates/{id}/availability?start=2026-08-05T09:00:00&end=2026-08-10T18:00:00
     *
     * <p>Возвращает:
     * <ul>
     *   <li>{@code available} — true/false</li>
     *   <li>{@code count} — количество свободных экземпляров</li>
     * </ul>
     *
     * @param id    ID шаблона (ToolTemplate)
     * @param start начало желаемого периода аренды (ISO DateTime)
     * @param end   конец желаемого периода аренды (ISO DateTime)
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable java.util.UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        int count = availabilityService.getAvailableCount(id, start, end);
        return ResponseEntity.ok(Map.of(
                "templateId", id,
                "startDate", start,
                "endDate", end,
                "available", count > 0,
                "count", count
        ));
    }
}