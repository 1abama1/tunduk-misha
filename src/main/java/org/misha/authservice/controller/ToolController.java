package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.*;
import org.misha.authservice.entity.ToolImage;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.service.ToolAvailabilityService;
import org.misha.authservice.service.ToolService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tools")
@RequiredArgsConstructor
public class ToolController {
    private final ToolService toolService;
    private final ToolAvailabilityService availabilityService;

    // Получить все инструменты (новый DTO-слой с фильтрами)
    @GetMapping
    public List<ToolListDto> getTools(
            @RequestParam(required = false) ToolStatus status,
            @RequestParam(required = false) Long categoryId
    ) {
        return toolService.getFiltered(status, categoryId);
    }

    // Получить все инструменты (старый метод для обратной совместимости)
    @GetMapping("/all")
    public ResponseEntity<List<ToolDto>> getAllTools() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    // Получить инструмент по ID
    @GetMapping("/{id}")
    public ResponseEntity<ToolDto> getTool(@PathVariable Long id) {
        return ResponseEntity.ok(toolService.getToolById(id));
    }

    // Создать инструмент
    @PostMapping
    public ResponseEntity<ToolDto> createTool(@RequestBody CreateToolRequest request) {
        ToolDto tool = toolService.createTool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tool);
    }

    // Обновить инструмент
    @PutMapping("/{id}")
    public ResponseEntity<ToolListDto> updateTool(
            @PathVariable Long id,
            @RequestBody UpdateToolRequest req
    ) {
        return ResponseEntity.ok(toolService.update(id, req));
    }

    // Обновить статус инструмента
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateToolStatus(
            @PathVariable Long id,
            @RequestBody UpdateToolStatusRequest request) {
        toolService.updateToolStatus(id, request);
        return ResponseEntity.ok().build();
    }

    // Удалить инструмент
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTool(@PathVariable Long id) {
        toolService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }

    // Получить доступные инструменты (для договора)
    @GetMapping("/available")
    public ResponseEntity<List<ToolDto>> getAvailableTools(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long rentalPointId) {
        return ResponseEntity.ok(toolService.getAvailableTools(categoryId, rentalPointId));
    }

    // Загрузить изображения инструмента
    @PostMapping("/{toolId}/images")
    public ResponseEntity<List<ToolImageDto>> uploadToolImages(
            @PathVariable Long toolId,
            @RequestParam("files") MultipartFile[] files) {
        List<ToolImage> images = toolService.uploadToolImages(toolId, files);
        List<ToolImageDto> imageDtos = images.stream()
                .map(img -> new ToolImageDto(img.getId(), img.getFileName(), img.getContentType()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(imageDtos);
    }

    // Получить изображение инструмента
    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> getToolImage(@PathVariable Long imageId) {
        byte[] imageData = toolService.getToolImage(imageId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(imageData);
    }

    // Удалить изображение инструмента
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<?> deleteToolImage(@PathVariable Long imageId) {
        toolService.deleteToolImage(imageId);
        return ResponseEntity.noContent().build();
    }

    // Старые эндпоинты для обратной совместимости
    @GetMapping("/all-old")
    public ResponseEntity<List<ToolDto>> getAllToolsOld() {
        return ResponseEntity.ok(toolService.getAllTools());
    }

    @GetMapping("/today")
    public ResponseEntity<List<ToolDto>> getTodayTools() {
        return ResponseEntity.ok(toolService.getTodayTools());
    }

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