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
 * РљРѕРЅС‚СЂРѕР»Р»РµСЂ РґР»СЏ СѓРїСЂР°РІР»РµРЅРёСЏ Р°СЂРµРЅРґРѕР№ РёРЅСЃС‚СЂСѓРјРµРЅС‚РѕРІ.
 * Р РµР°Р»РёР·СѓРµС‚ РїСЂР°РІРёР»СЊРЅСѓСЋ Р°СЂС…РёС‚РµРєС‚СѓСЂСѓ: Р°СЂРµРЅРґР° РїСЂРѕРёСЃС…РѕРґРёС‚ РЅР° СѓСЂРѕРІРЅРµ РєРѕРЅРєСЂРµС‚РЅРѕРіРѕ
 * СЌРєР·РµРјРїР»СЏСЂР° (ToolInstance).
 */
@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /**
     * РђСЂРµРЅРґР° РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°.
     * РЎРѕР·РґР°РµС‚ РґРѕРіРѕРІРѕСЂ Р°СЂРµРЅРґС‹ Рё РїСЂРёРІСЏР·С‹РІР°РµС‚ РєРѕРЅРєСЂРµС‚РЅС‹Р№ СЌРєР·РµРјРїР»СЏСЂ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р° Рє
     * РґРѕРіРѕРІРѕСЂСѓ.
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
     * Р’РѕР·РІСЂР°С‚ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°.
     * РћСЃРІРѕР±РѕР¶РґР°РµС‚ РёРЅСЃС‚СЂСѓРјРµРЅС‚ РѕС‚ РґРѕРіРѕРІРѕСЂР° Рё Р·Р°РєСЂС‹РІР°РµС‚ РґРѕРіРѕРІРѕСЂ.
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
                "message", "ToolInstance returned successfully"));
    }

    /**
     * РџСЂРµРѕР±СЂР°Р·СѓРµС‚ RentalDocument РІ DTO.
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
                doc.getComment(),
                doc.getOfflineId());
    }
}

