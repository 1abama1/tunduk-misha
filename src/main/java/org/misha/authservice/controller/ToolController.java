package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.*;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.service.ToolAvailabilityService;
import org.misha.authservice.service.ToolHistoryService;
import org.misha.authservice.service.ToolService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @Deprecated(forRemoval = true)
    @GetMapping("/filtered")
    public List<ToolListDto> getTools(
            @RequestParam(required = false) ToolStatus status,
            @RequestParam(required = false) Long categoryId
    ) {
        return toolService.getFiltered(status, categoryId);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ToolDto>> getAllTools() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    @PostMapping
    public ToolDtoSimple create(@Valid @RequestBody CreateToolRequest request) {
        return toolService.create(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ToolListDto> updateTool(
            @PathVariable Long id,
            @RequestBody UpdateToolRequest req
    ) {
        return ResponseEntity.ok(toolService.update(id, req));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateToolStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateToolStatusRequest request) {
        toolService.updateToolStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTool(@PathVariable Long id) {
        toolService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<ToolDto>> getAvailableTools(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long rentalPointId) {
        return ResponseEntity.ok(toolService.getAvailableTools(categoryId, rentalPointId));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ToolDto>> getToolsByTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(toolService.getByTemplate(templateId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<org.misha.authservice.dto.tool.ToolHistoryDto>> getHistory(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(toolHistoryService.getHistory(id));
    }

    @PostMapping("/{toolId}/images")
    public ResponseEntity<List<ToolImageDto>> uploadToolImages(
            @PathVariable Long toolId,
            @RequestParam("files") MultipartFile[] files) {
        List<ToolImageDto> imageDtos = toolService.uploadToolImages(toolId, files).stream()
                .map(img -> new ToolImageDto(img.getId(), img.getFileName(), img.getContentType()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(imageDtos);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getToolImage(@PathVariable Long imageId) {
        byte[] imageData = toolService.getToolImage(imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(imageData);
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteToolImage(@PathVariable Long imageId) {
        toolService.deleteToolImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/all-old")
    public ResponseEntity<List<ToolDto>> getAllToolsOld() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    @GetMapping("/today")
    public ResponseEntity<List<ToolDto>> getTodayTools() {
        return ResponseEntity.ok(toolService.getTodayTools());
    }

    @Deprecated(forRemoval = true)
    @GetMapping("/available/old")
    public ResponseEntity<List<AvailableToolDto>> getAvailableToolsOld(@RequestParam Long templateId) {
        List<AvailableToolDto> tools = availabilityService.getAvailableTools(templateId)
                .stream()
                .map(tool -> new AvailableToolDto(
                        tool.getId(),
                        tool.getTemplate() != null ? tool.getTemplate().getName() : null,
                        tool.getSerialNumber() != null ? tool.getSerialNumber() : tool.getInventoryNumber()
                ))
                .toList();
        return ResponseEntity.ok(tools);
    }
}
