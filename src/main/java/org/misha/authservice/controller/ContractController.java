package org.misha.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.AvailableToolDto;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.TerminateContractRequest;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.entity.ToolTemplate;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.misha.authservice.service.ContractService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ToolRepository toolRepository;
    private final ToolTemplateRepository templateRepository;

    @GetMapping("/available")
    public List<AvailableToolDto> getAvailable(@RequestParam Long templateId) {
        ToolTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BadRequestException("Template not found"));

        return toolRepository.findByTemplateIdAndContractIsNull(templateId)
                .stream()
                .map(t -> new AvailableToolDto(t.getId(), template.getName(), t.getSerialNumber()))
                .toList();
    }

    @PostMapping("/create")
    public ResponseEntity<RentalDocumentDto> create(@RequestBody CreateContractRequest req) {
        return ResponseEntity.ok(contractService.createContract(req));
    }

    @GetMapping
    public ResponseEntity<List<RentalDocumentDto>> getAll() {
        return ResponseEntity.ok(contractService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalDocumentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    @PostMapping("/{id}/close")

    public ResponseEntity<?> close(@PathVariable Long id) {
        contractService.closeContract(id);
        return ResponseEntity.ok(Map.of(
                "status", "closed",
                "contractId", id
        ));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<?> terminate(
            @PathVariable Long id,
            @RequestBody TerminateContractRequest req
    ) {
        contractService.terminateContract(id, req.reason());
        return ResponseEntity.ok(Map.of(
                "status", "terminated",
                "contractId", id
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RentalDocumentDto> updateContract(
            @PathVariable Long id,
            @RequestBody UpdateContractRequest req
    ) {
        return ResponseEntity.ok(contractService.update(id, req));
    }

    @PostMapping("/excel")
    public ResponseEntity<byte[]> generateExcelContract(@RequestBody ContractRequest request) {
        try {
            byte[] file = contractService.generateContractFileAndMarkClient(request);
            String fileName = "contract_" + (request.getContractNumber() == null
                    ? System.currentTimeMillis()
                    : request.getContractNumber()) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
        } catch (BadRequestException bre) {
            throw bre;
        } catch (Exception ex) {
            throw new RuntimeException("Не удалось сгенерировать файл: " + ex.getMessage(), ex);
        }
    }

}
