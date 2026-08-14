package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateToolBatchRequest;
import org.misha.authservice.dto.CreateToolRequest;
import org.misha.authservice.dto.AvailableToolDto;
import org.misha.authservice.dto.ToolDto;
import org.misha.authservice.dto.ToolListDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.dto.UpdateToolRequest;
import org.misha.authservice.dto.UpdateToolStatusRequest;
import org.misha.authservice.entity.*;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.mapper.ToolMapper;
import org.misha.authservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolService {
    private final ToolInstanceRepository toolInstanceRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolBookingRepository bookingRepository;
    private final ToolMapper toolMapper;
    private final ToolRentalGuard toolRentalGuard;

    @Transactional(readOnly = true)
    public List<ToolListDto> getAllList() {
        return toolInstanceRepository.findAll()
                .stream()
                .map(toolMapper::toListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getAllTools() {
        return toolInstanceRepository.findAll().stream()
                .map(t -> ToolDto.fromEntity(t))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ToolDtoSimple> getAll() {
        return toolInstanceRepository.findAll().stream()
                .map(ToolDtoSimple::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ToolDtoSimple getOne(Long id) {
        return ToolDtoSimple.fromEntity(
                toolInstanceRepository.findById(id)
                        .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND))
        );
    }

    @Transactional(readOnly = true)
    public ToolDto getOneFull(Long id) {
        return ToolDto.fromEntity(toolInstanceRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public ToolDto getToolById(Long id) {
        return getOneFull(id);
    }

    @Transactional
    public ToolDtoSimple create(CreateToolRequest request) {
        ToolInstance savedTool = createToolEntity(request);
        return ToolDtoSimple.fromEntity(savedTool);
    }

    @Transactional
    public ToolDto createTool(CreateToolRequest request) {
        ToolInstance savedTool = createToolEntity(request);
        return ToolDto.fromEntity(savedTool);
    }

    private ToolInstance createToolEntity(CreateToolRequest request) {
        ToolTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found",
                        HttpStatus.NOT_FOUND));

        String invNum = request.inventoryNumber();
        if (invNum != null && invNum.trim().isEmpty()) {
            invNum = null;
        }

        if (invNum != null) {
            if (toolInstanceRepository.existsByInventoryNumber(invNum)) {
                throw new AppException(
                        "INVENTORY_EXISTS",
                        "Inventory number must be unique",
                        HttpStatus.CONFLICT);
            }
        }

        Integer maxInstanceNum = toolInstanceRepository.findMaxInstanceNumberByTemplateId(template.getId());
        int nextInstanceNum = maxInstanceNum == null ? 1 : maxInstanceNum + 1;

        ToolInstance tool = ToolInstance.builder()
                .template(template)
                .inventoryNumber(invNum)
                .instanceNumber(nextInstanceNum)
                .build();

        return toolInstanceRepository.save(tool);
    }

    @Transactional
    public List<ToolDto> createToolsInBatch(CreateToolBatchRequest request) {
        ToolTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found: " + request.templateId(),
                        HttpStatus.NOT_FOUND));

        String maxInventory = toolInstanceRepository
                .findMaxInventoryNumberByTemplateId(request.templateId());

        String prefix = "";
        int nextNumber = 1;

        if (maxInventory != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^(.*?)(\\d+)$")
                    .matcher(maxInventory.trim());
            if (m.matches()) {
                prefix = m.group(1);
                nextNumber = Integer.parseInt(m.group(2)) + 1;
            } else {
                prefix = maxInventory + "-";
                nextNumber = 1;
            }
        }

        Integer maxInstanceNum = toolInstanceRepository.findMaxInstanceNumberByTemplateId(template.getId());
        int nextInstanceNum = maxInstanceNum == null ? 1 : maxInstanceNum + 1;

        int padLength = Math.max(3, String.valueOf(nextNumber + request.count()).length());
        List<ToolInstance> created = new ArrayList<>(request.count());
        for (int i = 0; i < request.count(); i++) {
            String inventoryNumber = prefix + String.format("%0" + padLength + "d", nextNumber + i);

            while (toolInstanceRepository.existsByInventoryNumber(inventoryNumber)) {
                nextNumber++;
                inventoryNumber = prefix + String.format("%0" + padLength + "d", nextNumber + i);
            }

            ToolInstance tool = ToolInstance.builder()
                    .template(template)
                    .inventoryNumber(inventoryNumber)
                    .instanceNumber(nextInstanceNum + i)
                    .build();

            created.add(toolInstanceRepository.save(tool));
        }

        return created.stream().map(t -> ToolDto.fromEntity(t)).collect(Collectors.toList());
    }

    @Transactional
    public void deleteTool(Long id) {
        ToolInstance tool = toolInstanceRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND));

        toolRentalGuard.ensureCanDelete(tool);
        toolInstanceRepository.delete(tool);
    }

    @Transactional(readOnly = true)
    public List<ToolDto> getByTemplate(UUID templateId) {
        Map<Long, UUID> activeBookings = bookingRepository.findByTemplateId(templateId).stream()
                .filter(b -> b.getStatus() == BookingStatus.ACTIVE && b.getEndDateTime().isAfter(java.time.LocalDateTime.now()))
                .collect(Collectors.toMap(
                        b -> b.getToolInstance().getId(),
                        ToolBooking::getId,
                        (existing, replacement) -> existing
                ));

        return toolInstanceRepository.findByTemplateId(templateId)
                .stream()
                .map(t -> ToolDto.fromEntity(t, activeBookings.get(t.getId())))
                .toList();
    }

    @Transactional
    public ToolDto updateStatus(Long id, UpdateToolStatusRequest request) {
        ToolInstance tool = toolInstanceRepository.findById(id)
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

        ToolInstanceStatus newStatus;
        try {
            newStatus = ToolInstanceStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new AppException("INVALID_STATUS", "Invalid status: " + request.status(), HttpStatus.BAD_REQUEST);
        }

        // Only allow status change if not rented
        if (tool.getContract() != null && newStatus != ToolInstanceStatus.AVAILABLE) {
            throw new AppException("TOOL_RENTED", "Cannot change status of a rented tool", HttpStatus.BAD_REQUEST);
        }

        tool.setStatus(newStatus);
        return ToolDto.fromEntity(toolInstanceRepository.save(tool));
    }
}
