package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateTemplateRequest;
import org.misha.authservice.dto.TemplateDto;
import org.misha.authservice.dto.TemplateFullDto;
import org.misha.authservice.dto.ToolDtoSimple;
import org.misha.authservice.entity.BookingStatus;
import org.misha.authservice.entity.ToolBooking;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.repository.ToolBookingRepository;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolTemplateService {
    private final ToolTemplateRepository templateRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolBookingRepository bookingRepository;

    @Transactional
    public TemplateDto create(CreateTemplateRequest request) {
        ToolCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Category not found"));

        ToolTemplate template = templateRepository.save(
                ToolTemplate.builder()
                        .name(request.name())
                        .category(category)
                        .dailyRentalPrice(request.dailyRentalPrice())
                        .depositAmount(request.depositAmount())
                        .purchasePrice(request.purchasePrice())
                        .build()
        );

        return new TemplateDto(template.getId(), template.getName(), category.getId());
    }

    @Transactional(readOnly = true)
    public List<TemplateDto> getByCategory(UUID categoryId) {
        return templateRepository.findByCategoryId(categoryId)
                .stream()
                .map(t -> new TemplateDto(t.getId(), t.getName(), categoryId))
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateFullDto getFull(UUID id) {
        ToolTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Template not found"));

        List<ToolInstance> tools = ToolInstanceRepository.findByTemplateId(template.getId());
        Map<Long, UUID> activeBookings = getActiveBookingsMap();

        return new TemplateFullDto(
                template.getId(),
                template.getName(),
                template.getCategory().getId(),
                template.getDailyRentalPrice(),
                template.getDepositAmount(),
                template.getPurchasePrice(),
                tools.stream()
                        .map(t -> ToolDtoSimple.fromEntity(t, activeBookings.get(t.getId())))
                        .toList()
        );
    }

    @Transactional
    public TemplateFullDto update(UUID id, org.misha.authservice.dto.UpdateTemplateRequest request) {
        ToolTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Template not found"));

        ToolCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Category not found"));

        template.setName(request.name());
        template.setCategory(category);
        template.setDailyRentalPrice(request.dailyRentalPrice());
        template.setDepositAmount(request.depositAmount());
        template.setPurchasePrice(request.purchasePrice());

        templateRepository.save(template);

        return getFull(id);
    }

    private Map<Long, UUID> getActiveBookingsMap() {
        return bookingRepository.findByStatus(BookingStatus.ACTIVE).stream()
                .filter(b -> b.getEndDateTime().isAfter(java.time.LocalDateTime.now()))
                .collect(Collectors.toMap(
                        b -> b.getToolInstance().getId(),
                        ToolBooking::getId,
                        (existing, replacement) -> existing
                ));
    }
}
