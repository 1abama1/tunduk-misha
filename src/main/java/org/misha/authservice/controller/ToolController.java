package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ToolDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.dto.CreateToolRequest;
import org.misha.authservice.dto.CreateToolBatchRequest;
import org.misha.authservice.dto.UpdateToolStatusRequest;
import org.misha.authservice.dto.tool.ToolHistoryDto;
import org.misha.authservice.service.ToolAvailabilityService;
import org.misha.authservice.service.ToolHistoryService;
import org.misha.authservice.service.ToolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {
    private final ToolService toolService;
    private final ToolAvailabilityService availabilityService;
    private final ToolHistoryService toolHistoryService;

    @GetMapping
    public List<ToolDtoSimple> getAll() {
        return toolService.getAll();
    }

    @GetMapping("/{id}")
    public ToolDtoSimple getOne(@PathVariable Long id) {
        return toolService.getOne(id);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ToolDto>> getAllTools() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    @PostMapping
    public ToolDtoSimple create(@Valid @RequestBody CreateToolRequest request) {
        return toolService.create(request);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ToolDto>> createBatch(
            @Valid @RequestBody CreateToolBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toolService.createToolsInBatch(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTool(@PathVariable Long id) {
        toolService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ToolDto>> getToolsByTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(toolService.getByTemplate(templateId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ToolHistoryDto>> getHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(toolHistoryService.getHistory(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ToolDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateToolStatusRequest request) {
        return ResponseEntity.ok(toolService.updateStatus(id, request));
    }
}
