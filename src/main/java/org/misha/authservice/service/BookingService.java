package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.BookingDto;
import org.misha.authservice.dto.CreateBookingRequest;
import org.misha.authservice.entity.BookingStatus;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ToolBooking;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.ToolBookingRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ToolBookingRepository bookingRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolInstanceRepository instanceRepository;
    private final ToolAvailabilityService availabilityService;
    private final BookingTimeCalculator timeCalculator;

    @Transactional
    public BookingDto createBooking(CreateBookingRequest request) {
            LocalDateTime calculatedEndDateTime = timeCalculator.calculateEndDateTime(request.startDateTime(), request.hours());
        
        if (request.startDateTime().isAfter(calculatedEndDateTime)) {
            throw new AppException("INVALID_DATES", "Start date must be before end date", HttpStatus.BAD_REQUEST);
        }

        ToolTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));

        ToolInstance toolInstance = instanceRepository.findById(request.toolInstanceId())
                .orElseThrow(() -> new AppException("INSTANCE_NOT_FOUND", "Tool instance not found", HttpStatus.NOT_FOUND));

        if (!toolInstance.getTemplate().getId().equals(template.getId())) {
            throw new AppException("INVALID_TEMPLATE", "Tool instance does not belong to the specified template", HttpStatus.BAD_REQUEST);
        }

        // Check availability
        boolean available = availabilityService.isInstanceAvailableForPeriod(
                request.toolInstanceId(), request.startDateTime(), calculatedEndDateTime);

        if (!available) {
            throw new AppException("TOOL_NOT_AVAILABLE", 
                    "The requested tool instance is not available for this period", HttpStatus.CONFLICT);
        }

        ToolBooking booking = ToolBooking.builder()
                .clientName(request.clientName())
                .clientPhone(request.clientPhone())
                .template(template)
                .toolInstance(toolInstance)
                .startDateTime(request.startDateTime())
                .endDateTime(calculatedEndDateTime)
                .comment(request.comment())
                .status(BookingStatus.ACTIVE)
                .build();

        return BookingDto.fromEntity(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getAllBookings() {
        return bookingRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto cancelBooking(UUID id) {
        ToolBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new AppException("INVALID_STATUS", "Only active bookings can be cancelled", HttpStatus.BAD_REQUEST);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return BookingDto.fromEntity(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getByTemplate(UUID templateId) {
        return bookingRepository.findByTemplateId(templateId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getByToolInstance(Long toolInstanceId) {
        return bookingRepository.findByToolInstanceId(toolInstanceId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .map(BookingDto::fromEntity)
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void deleteBooking(UUID id) {
        if (!bookingRepository.existsById(id)) {
            throw new AppException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND);
        }
        bookingRepository.deleteById(id);
    }
}
