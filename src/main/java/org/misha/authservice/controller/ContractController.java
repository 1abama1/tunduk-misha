package org.misha.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.AvailableToolDto;
import org.misha.authservice.dto.CloseContractRequest;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.service.ContractService;
import org.misha.authservice.service.ToolService;
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
    private final ToolService toolService;

    @GetMapping("/available")
    public List<AvailableToolDto> getAvailable(@RequestParam Long templateId) {
        return toolService.getAvailableByTemplate(templateId);
    }

    @PostMapping("/create")
    public ResponseEntity<RentalDocumentDto> create(@Valid @RequestBody CreateContractRequest req) {
        return ResponseEntity.ok(contractService.createContract(req));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<RentalDocumentDto>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(contractService.getAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalDocumentDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getById(id));
    }

    @GetMapping("/active-table")
    public ResponseEntity<List<ActiveContractRowDto>> getActiveContractsTable() {
        return ResponseEntity.ok(contractService.getActiveContractsTable());
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id, @RequestBody(required = false) CloseContractRequest req) {
        contractService.closeContract(id, req);
        return ResponseEntity.ok(Map.of(
                "status", "closed",
                "contractId", id));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        contractService.restoreContract(id);
        return ResponseEntity.ok(Map.of(
                "status", "restored",
                "contractId", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RentalDocumentDto> updateContract(
            @PathVariable Long id,
            @RequestBody UpdateContractRequest req) {
        return ResponseEntity.ok(contractService.update(id, req));
    }

    /**
     * Генерирует Excel файл договора по ID договора.
     * Использует новый ExcelGeneratorService с точным маппингом ячеек по
     * инструкции.
     * 
     * @param id ID договора
     * @return Excel файл
     */
    @GetMapping(value = { "/{id}/excel",
            "/{id}/excel/" }, produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> generateExcelContractById(@PathVariable Long id) {
        try {
            byte[] file = contractService.generateContractExcelById(id);
            String fileName = "contract_" + id + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(file);
        } catch (Exception ex) {
            throw new RuntimeException("Не удалось сгенерировать файл: " + ex.getMessage(), ex);
        }
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
