package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.RentRequest;
import org.misha.authservice.dto.ReturnRequest;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.service.RentalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Контроллер для управления арендой инструментов.
 * Реализует правильную архитектуру: аренда происходит на уровне конкретного
 * экземпляра (Tool).
 */
@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /**
     * Аренда инструмента.
     * Создает договор аренды и привязывает конкретный экземпляр инструмента к
     * договору.
     *
     * POST /api/rent
     * Body: {
     * "toolId": 123,
     * "clientId": 88,
     * "rentDays": 3,
     * "pricePerDay": 2000
     * }
     */
    @PostMapping
    public ResponseEntity<RentalDocumentDto> rentTool(@Valid @RequestBody RentRequest req) {
        RentalDocument doc = rentalService.rentTool(req);
        return ResponseEntity.ok(toDto(doc));
    }

    /**
     * Возврат инструмента.
     * Освобождает инструмент от договора и закрывает договор.
     *
     * POST /api/rent/return
     * Body: {
     * "contractId": 123
     * }
     */
    @PostMapping("/return")
    public ResponseEntity<Map<String, Object>> returnTool(@Valid @RequestBody ReturnRequest req) {
        rentalService.returnTool(req);
        return ResponseEntity.ok(Map.of(
                "status", "returned",
                "contractId", req.contractId(),
                "message", "Tool returned successfully"));
    }

    /**
     * Преобразует RentalDocument в DTO.
     */
    private RentalDocumentDto toDto(RentalDocument doc) {
        return new RentalDocumentDto(
                doc.getId(),
                doc.getContractNumber(),
                doc.getStartDateTime(),
                doc.getDailyPrice(),
                doc.getAmount(),
                doc.getCreatedAt(),
                doc.getClient() != null ? doc.getClient().getId() : null,
                doc.getReturnDate(),
                doc.getTerminatedAt(),
                doc.getTerminationReason(),
                doc.getStatus(),
                doc.getComment());
    }
}
