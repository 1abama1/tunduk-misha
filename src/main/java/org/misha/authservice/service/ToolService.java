package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateToolRequest;
import org.misha.authservice.dto.ToolAttributeDto;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<ToolListDto> getAll() {
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
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getTodayTools() {
        return toolRepository.findCreatedToday().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ToolDto getToolById(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));
        return toDto(tool);
    }

    @Transactional
    public ToolDto createTool(CreateToolRequest request) {
        // Проверка уникальности инвентарного номера (если указан)
        if (request.getInventoryNumber() != null && !request.getInventoryNumber().trim().isEmpty()) {
            if (toolRepository.existsByInventoryNumber(request.getInventoryNumber())) {
                throw new AppException("INVENTORY_NUMBER_EXISTS", "Inventory number already exists", HttpStatus.BAD_REQUEST);
            }
        }

        Tool.ToolBuilder builder = Tool.builder()
                .name(request.getName())
                .article(request.getArticle())
                .inventoryNumber(request.getInventoryNumber())
                .description(request.getDescription())
                .purchasePrice(request.getPurchasePrice())
                .purchaseDate(request.getPurchaseDate())
                .deposit(request.getDeposit());

        // Установка статуса (по умолчанию AVAILABLE)
        if (request.getStatus() != null && request.getStatus() != ToolStatus.RENTED) {
            builder.status(request.getStatus());
        } else {
            builder.status(ToolStatus.AVAILABLE);
        }

        // Установка категории
        if (request.getCategoryId() != null) {
            ToolCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.NOT_FOUND));
            builder.category(category);
        }

        // Установка пункта проката
        if (request.getRentalPointId() != null) {
            RentalPoint rentalPoint = rentalPointRepository.findById(request.getRentalPointId())
                    .orElseThrow(() -> new AppException("RENTAL_POINT_NOT_FOUND", "Rental point not found", HttpStatus.NOT_FOUND));
            builder.rentalPoint(rentalPoint);
        }

        // Обратная совместимость с template
        if (request.getTemplateId() != null) {
            ToolTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));
            builder.template(template);
            if (request.getSerialNumber() != null) {
                builder.serialNumber(request.getSerialNumber());
            }
        }

        // Установка договора (если есть)
        if (request.getContractId() != null) {
            RentalDocument contract = rentalDocumentRepository.findById(request.getContractId())
                    .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Contract not found", HttpStatus.NOT_FOUND));
            builder.contract(contract);
            builder.status(ToolStatus.RENTED); // Автоматически RENTED при наличии договора
        }

        Tool tool = builder.build();

        // Добавление атрибутов
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            List<ToolAttribute> attributes = request.getAttributes().stream()
                    .map(attr -> ToolAttribute.builder()
                            .name(attr.getName())
                            .value(attr.getValue())
                            .tool(tool)
                            .build())
                    .collect(Collectors.toList());
            tool.setAttributes(attributes);
        }

        Tool savedTool = toolRepository.save(tool);
        return toDto(savedTool);
    }

    @Transactional
    public ToolListDto update(Long id, UpdateToolRequest req) {
        Tool tool = toolRepository.findByIdWithTemplateAndContract(id)
                .orElseThrow(() -> new AppException(
                        "TOOL_NOT_FOUND",
                        "Инструмент не найден",
                        HttpStatus.NOT_FOUND
                ));

        ToolStatus status = resolveStatus(tool);

        if (status == ToolStatus.RENTED || status == ToolStatus.OVERDUE) {
            throw new AppException(
                    "TOOL_IN_USE",
                    "Нельзя редактировать инструмент в аренде",
                    HttpStatus.BAD_REQUEST
            );
        }

        // обновляем TEMPLATE
        if (req.name() != null) {
            if (tool.getTemplate() != null) {
                tool.getTemplate().setName(req.name());
                templateRepository.save(tool.getTemplate());
            } else {
                tool.setName(req.name());
            }
        }
        if (req.description() != null) {
            if (tool.getTemplate() != null) {
                tool.getTemplate().setDescription(req.description());
                templateRepository.save(tool.getTemplate());
            } else {
                tool.setDescription(req.description());
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
            } else {
                tool.setCategory(category);
            }
        }

        // обновляем сам Tool
        if (req.deposit() != null) {
            tool.setDeposit(req.deposit().doubleValue());
        }
        if (req.purchaseDate() != null) {
            tool.setPurchaseDate(req.purchaseDate());
        }
        if (req.purchasePrice() != null) {
            tool.setPurchasePrice(req.purchasePrice().doubleValue());
        }

        toolRepository.save(tool);
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
        if (tool.getContract() != null) {
            throw new AppException("TOOL_IS_RENTED", "Cannot change status of rented tool", HttpStatus.BAD_REQUEST);
        }

        tool.setStatus(request.status());
        toolRepository.save(tool);
    }

    @Transactional
    public void deleteTool(Long id) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

        // Нельзя удалить инструмент, который в аренде
        if (tool.getContract() != null) {
            throw new AppException("CANNOT_DELETE_RENTED_TOOL", "Cannot delete tool that is currently rented", HttpStatus.BAD_REQUEST);
        }

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
                images.add(imageRepository.save(image));
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
                .description(tool.getDescription())
                .purchasePrice(tool.getPurchasePrice())
                .purchaseDate(tool.getPurchaseDate())
                .deposit(tool.getDeposit())
                .createdAt(tool.getCreatedAt())
                .serialNumber(tool.getSerialNumber());

        if (tool.getCategory() != null) {
            builder.categoryId(tool.getCategory().getId())
                    .categoryName(tool.getCategory().getName());
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

    private ToolStatus resolveStatus(Tool tool) {
        if (tool.getContract() == null) {
            return ToolStatus.AVAILABLE;
        }

        if (tool.getContract().getTerminatedAt() != null) {
            return ToolStatus.AVAILABLE;
        }

        if (tool.getContract().getExpectedReturnDate() != null &&
            tool.getContract().getExpectedReturnDate().isBefore(LocalDate.now())) {
            return ToolStatus.OVERDUE;
        }

        return ToolStatus.RENTED;
    }
}