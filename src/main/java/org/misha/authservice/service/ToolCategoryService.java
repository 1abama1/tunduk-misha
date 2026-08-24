package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CategoryDto;
import org.misha.authservice.dto.CategoryFullDto;
import org.misha.authservice.dto.CreateCategoryRequest;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.entity.ToolBooking;
import org.misha.authservice.entity.BookingStatus;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.misha.authservice.repository.ToolBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolCategoryService {
    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolBookingRepository bookingRepository;

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        ToolCategory category = categoryRepository.save(
                ToolCategory.builder().name(request.name()).build()
        );
        return new CategoryDto(category.getId(), category.getName());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryFullDto getFull(UUID id) {
        ToolCategory category = categoryRepository.findByIdWithTemplatesAndTools(id)
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Category not found"));

        Map<Long, UUID> activeBookings = getActiveBookingsMap();

        List<TemplateFullDto> templateDtos = category.getTemplates().stream().map(t -> {
            List<ToolDtoSimple> toolDtos = t.getInstances().stream()
                .map(tool -> ToolDtoSimple.fromEntity(tool, activeBookings.get(tool.getId())))
                .toList();
            return new TemplateFullDto(t.getId(), t.getName(), category.getId(), t.getDailyRentalPrice(), t.getDepositAmount(), t.getPurchasePrice(), toolDtos);
        }).toList();

        return new CategoryFullDto(category.getId(), category.getName(), templateDtos);
    }

    @Transactional(readOnly = true)
    public List<CategoryFullDto> getAllFull() {
        List<ToolCategory> categories = categoryRepository.findAllWithTemplatesAndTools();
        Map<Long, UUID> activeBookings = getActiveBookingsMap();

        return categories.stream().map(category -> {
            List<TemplateFullDto> templateDtos = category.getTemplates().stream().map(t -> {
            List<ToolDtoSimple> toolDtos = t.getInstances().stream()
                .map(tool -> ToolDtoSimple.fromEntity(tool, activeBookings.get(tool.getId())))
                .toList();
                return new TemplateFullDto(t.getId(), t.getName(), category.getId(), t.getDailyRentalPrice(), t.getDepositAmount(), t.getPurchasePrice(), toolDtos);
            }).toList();
            return new CategoryFullDto(category.getId(), category.getName(), templateDtos);
        }).toList();
    }

    private Map<Long, UUID> getActiveBookingsMap() {
        return bookingRepository.findByStatus(BookingStatus.ACTIVE).stream()
                .filter(b -> b.getEndDateTime().isAfter(java.time.LocalDateTime.now()))
                .collect(Collectors.toMap(
                        b -> b.getToolInstance().getId(),
                        ToolBooking::getId,
                        (existing, replacement) -> existing // In case of multiple active bookings, keep the first one found
                ));
    }
}
