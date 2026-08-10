package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.BookingDto;
import org.misha.authservice.dto.CreateBookingRequest;
import org.misha.authservice.entity.BookingStatus;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ToolBooking;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.ToolBookingRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ToolBookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final ToolTemplateRepository templateRepository;
    private final ToolAvailabilityService availabilityService;

    @Transactional
    public BookingDto createBooking(CreateBookingRequest request) {
        if (request.startDateTime().isAfter(request.endDateTime())) {
            throw new AppException("INVALID_DATES", "Start date must be before end date", HttpStatus.BAD_REQUEST);
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));

        ToolTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new AppException("TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND));

        // Check availability
        boolean available = availabilityService.isAvailableForPeriod(
                request.templateId(), request.startDateTime(), request.endDateTime());

        if (!available) {
            throw new AppException("TOOL_NOT_AVAILABLE", 
                    "No tools available for the requested period", HttpStatus.CONFLICT);
        }

        ToolBooking booking = ToolBooking.builder()
                .client(client)
                .template(template)
                .startDateTime(request.startDateTime())
                .endDateTime(request.endDateTime())
                .comment(request.comment())
                .status(BookingStatus.ACTIVE)
                .build();

        return BookingDto.fromEntity(bookingRepository.save(booking));
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
}
