package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.BookingDto;
import org.misha.authservice.dto.CreateBookingRequest;
import org.misha.authservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }

    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<BookingDto>> getByTemplate(@PathVariable UUID templateId) {
        return ResponseEntity.ok(bookingService.getByTemplate(templateId));
    }
}
