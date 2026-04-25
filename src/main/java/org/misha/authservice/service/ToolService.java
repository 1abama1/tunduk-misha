package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateToolRequest;
import org.misha.authservice.dto.ToolAttributeDto;
import org.misha.authservice.dto.AvailableToolDto;
import org.misha.authservice.dto.ToolDto;
import org.misha.authservice.dto.ToolImageDto;
import org.misha.authservice.dto.ToolListDto;
import org.misha.authservice.dto.ToolTemplateDto;
import org.misha.authservice.dto.UpdateToolRequest;
import org.misha.authservice.dto.UpdateToolStatusRequest;
import org.misha.authservice.entity.*;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.mapper.ToolMapper;
import org.misha.authservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolService {
    private final ToolRepository toolRepository;
    private final ToolTemplateRepository templateRepository;
    private final RentalDocumentRepository rentalDocumentRepository;
    private final ToolCategoryRepository categoryRepository;
    private final RentalPointRepository rentalPointRepository;
    private final ToolAttributeRepository attributeRepository;
    private final ToolImageRepository imageRepository;
    private final ToolMapper toolMapper;
    private final ToolRentalGuard toolRentalGuard;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ToolListDto> getAllList() {
        return toolRepository.findAllWithTemplate()
                .stream()
                .map(toolMapper::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ToolListDto> getFiltered(ToolStatus status, Long categoryId) {
        // Получаем все инструменты с нужными JOIN FETCH
        List<Tool> tools = toolRepository.findFiltered(null, categoryId);
        
        // Маппим и фильтруем по вычисленному status
        return tools.stream()
                .map(toolMapper::toListDto)
                .filter(dto -> status == null || dto.status() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getAllTools() {
        return toolRepository.findAll().stream()
                .map(ToolDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getTodayTools() {
        return toolRepository.findCreatedToday().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<org.misha.authservice.dto.ToolDtoSimple> getAll() {
        return toolRepository.findAll().stream()
                .map(org.misha.authservice.dto.ToolDtoSimple::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.misha.authservice.dto.ToolDtoSimple getOne(Long id) {
        return org.misha.authservice.dto.ToolDtoSimple.fromEntity(
                toolRepository.findById(id)
                        .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND))
        );
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getAllToolsForApi() {
        return toolRepository.findAll().stream()
                .map(ToolDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ToolDto getOneFull(Long id) {
        return ToolDto.fromEntity(toolRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public ToolDto getToolById(Long id) {
        return getOneFull(id);
    }

    @Transactional
    public org.misha.authservice.dto.ToolDtoSimple create(CreateToolRequest request) {
        Tool savedTool = createToolEntity(request);
        return org.misha.authservice.dto.ToolDtoSimple.fromEntity(savedTool);
    }

    @Transactional
    public ToolDto createTool(CreateToolRequest request) {
        Tool savedTool = createToolEntity(request);
        return ToolDto.fromEntity(savedTool);
    }

    private Tool createToolEntity(CreateToolRequest request) {
        ToolTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found",
                        HttpStatus.NOT_FOUND));

        if (request.inventoryNumber() != null && !request.inventoryNumber().trim().isEmpty()) {
            if (toolRepository.existsByInventoryNumber(request.inventoryNumber())) {
                throw new AppException(
                        "INVENTORY_EXISTS",
                        "Inventory number must be unique",
                        HttpStatus.CONFLICT);
            }
        }

        Tool tool = Tool.builder()
                .template(template)
                .name(request.name())
                .inventoryNumber(request.inventoryNumber())
                .article(request.article())
                .deposit(request.deposit())
                .purchasePrice(request.purchasePrice())
                .dailyPrice(request.dailyPrice())
                .status(ToolStatus.AVAILABLE)
                .build();

        Tool savedTool = toolRepository.save(tool);

        auditLogService.logCreate("Tool", savedTool.getId(), Map.of(
                "name", savedTool.getName() != null ? savedTool.getName() : "N/A",
                "inventoryNumber", savedTool.getInventoryNumber() != null ? savedTool.getInventoryNumber() : "N/A"
        ));

        return savedTool;
    }

    @Transactional
    public ToolListDto update(Long id, UpdateToolRequest req) {
        Tool tool = toolRepository.findByIdWithTemplateAndContract(id)
                .orElseThrow(() -> new AppException(
                        "TOOL_NOT_FOUND",
                        "Инструмент не найден",
                        HttpStatus.NOT_FOUND
                ));

        toolRentalGuard.ensureCanEdit(tool);

        // обновляем TEMPLATE
        if (req.name() != null) {
            if (tool.getTemplate() != null) {
                tool.getTemplate().setName(req.name());
                templateRepository.save(tool.getTemplate());
            } else {
                tool.setName(req.name());
            }
        }

        if (req.categoryId() != null) {
            ToolCategory category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new AppException(
                            "CATEGORY_NOT_FOUND",
                            "Категория не найдена",
                            HttpStatus.NOT_FOUND
                    ));
            if (tool.getTemplate() != null) {
                tool.getTemplate().setCategory(category);
                templateRepository.save(tool.getTemplate());
            }
        }

        // обновляем сам Tool
        if (req.deposit() != null) {
            tool.setDeposit(req.deposit().doubleValue());
        }
        if (req.dailyPrice() != null) {
            tool.setDailyPrice(req.dailyPrice().doubleValue());
        }
        if (req.purchasePrice() != null) {
            tool.setPurchasePrice(req.purchasePrice().doubleValue());
        }

        toolRepository.save(tool);

        // Audit logging
        Map<String, Object> changes = new java.util.HashMap<>();
        if (req.name() != null) changes.put("name", req.name());
        if (req.categoryId() != null) changes.put("categoryId", req.categoryId());
        if (req.dailyPrice() != null) changes.put("dailyPrice", req.dailyPrice());
        auditLogService.logUpdate("Tool", id, changes);

        return toolMapper.toListDto(tool);
    }

    @Transactional
    public void updateToolStatus(Long id, UpdateToolStatusRequest request) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

        // Нельзя вручную установить RENTED
        if (request.status() == ToolStatus.RENTED) {
            throw new AppException("CANNOT_SET_RENTED", "Cannot set RENTED status manually. It is set automatically when tool is assigned to contract.", HttpStatus.BAD_REQUEST);
        }

        // Если есть активный договор, нельзя изменить статус
        toolRentalGuard.ensureCanChangeStatus(tool);

        tool.setStatus(request.status());
        toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

        // Нельзя удалить инструмент, который в аренде
        toolRentalGuard.ensureCanDelete(tool);

        // Audit logging
        auditLogService.logDelete("Tool", id, Map.of(
                "name", tool.getName() != null ? tool.getName() : "N/A",
                "inventoryNumber", tool.getInventoryNumber() != null ? tool.getInventoryNumber() : "N/A"
        ));

        toolRepository.delete(tool);
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getAvailableTools(Long categoryId, Long rentalPointId) {
        List<Tool> tools = toolRepository.findAvailableTools(categoryId, rentalPointId);
        return tools.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ToolImage> uploadToolImages(Long toolId, MultipartFile[] files) {
        Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

        List<ToolImage> images = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                ToolImage image = ToolImage.builder()
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .data(file.getBytes())
                        .tool(tool)
                        .build();
                ToolImage savedImage = imageRepository.save(image);
                images.add(savedImage);

                // Audit logging
                auditLogService.logImageUpload("Tool", toolId, file.getOriginalFilename());
            } catch (IOException e) {
                throw new AppException("FILE_UPLOAD_ERROR", "Failed to upload file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return images;
    }

    @Transactional
    public void deleteToolImage(Long imageId) {
        ToolImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException("IMAGE_NOT_FOUND", "Image not found", HttpStatus.NOT_FOUND));
        imageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public byte[] getToolImage(Long imageId) {
        ToolImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException("IMAGE_NOT_FOUND", "Image not found", HttpStatus.NOT_FOUND));
        return image.getData();
    }

    private ToolDto toDto(Tool tool) {
        ToolDto.ToolDtoBuilder builder = ToolDto.builder()
                .id(tool.getId())
                .name(tool.getName())
                .article(tool.getArticle())
                .inventoryNumber(tool.getInventoryNumber())
                .status(tool.getStatus())
                .purchasePrice(tool.getPurchasePrice())
                .deposit(tool.getDeposit())
                .dailyPrice(tool.getDailyPrice())
                .createdAt(tool.getCreatedAt())
                .serialNumber(tool.getSerialNumber());

        // Категория берется из template
        if (tool.getTemplate() != null && tool.getTemplate().getCategory() != null) {
            builder.categoryId(tool.getTemplate().getCategory().getId())
                    .categoryName(tool.getTemplate().getCategory().getName());
        }

        if (tool.getRentalPoint() != null) {
            builder.rentalPointId(tool.getRentalPoint().getId())
                    .rentalPointName(tool.getRentalPoint().getName());
        }

        if (tool.getTemplate() != null) {
            ToolTemplateDto templateDto = new ToolTemplateDto();
            templateDto.setId(tool.getTemplate().getId());
            templateDto.setName(tool.getTemplate().getName());
            builder.template(templateDto);
        }

        // Атрибуты
        if (tool.getAttributes() != null && !tool.getAttributes().isEmpty()) {
            builder.attributes(tool.getAttributes().stream()
                    .map(attr -> new ToolAttributeDto(attr.getId(), attr.getName(), attr.getValue()))
                    .collect(Collectors.toList()));
        } else {
            builder.attributes(new ArrayList<>());
        }

        // Изображения
        if (tool.getImages() != null && !tool.getImages().isEmpty()) {
            builder.images(tool.getImages().stream()
                    .map(img -> new ToolImageDto(img.getId(), img.getFileName(), img.getContentType()))
                    .collect(Collectors.toList()));
        } else {
            builder.images(new ArrayList<>());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getByTemplate(Long templateId) {
        return toolRepository.findByTemplateId(templateId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableToolDto> getAvailableByTemplate(Long templateId) {
        ToolTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));

        return toolRepository.findByTemplateIdAndContractIsNull(templateId)
                .stream()
                .map(t -> new AvailableToolDto(t.getId(), template.getName(), t.getSerialNumber()))
                .toList();
    }

    private ToolStatus resolveStatus(Tool tool) {
        return toolRentalGuard.resolveStatus(tool);
    }
}