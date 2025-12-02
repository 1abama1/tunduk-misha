package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateDocumentRequest;
import org.misha.authservice.dto.DocumentDto;
import org.misha.authservice.dto.UpdateDocumentRequest;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.mapper.ClientMapper;
import org.misha.authservice.service.RentalDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class RentalDocumentController {

    private final RentalDocumentService service;
    private final ClientMapper clientMapper;

    // CREATE
    @PostMapping("/create")
    public ResponseEntity<DocumentDto> create(@RequestBody CreateDocumentRequest req) {
        RentalDocument doc = service.create(req);
        return ResponseEntity.ok(clientMapper.toDocDto(doc));
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAll() {
        return ResponseEntity.ok(
                service.findAll().stream()
                        .map(clientMapper::toDocDto)
                        .toList()
        );
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(clientMapper.toDocDto(service.findOne(id)));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DocumentDto> update(@PathVariable Long id,
                                              @RequestBody UpdateDocumentRequest req) {
        return ResponseEntity.ok(clientMapper.toDocDto(service.update(id, req)));
    }

    // CLOSE (возврат инструмента)
    @PostMapping("/{id}/close")
    public ResponseEntity<DocumentDto> close(@PathVariable Long id) {
        RentalDocument doc = service.close(id);
        return ResponseEntity.ok(clientMapper.toDocDto(doc));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
