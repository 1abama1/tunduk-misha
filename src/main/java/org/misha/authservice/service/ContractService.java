package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.TerminateContractRequest;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.exception.NotFoundException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final RentalDocumentRepository documentRepository;
    private final ContractExcelService contractExcelService;
    private final ContractValidator contractValidator;
    private final ToolRentalGuard toolRentalGuard;
    private final AuditLogService auditLogService;

    private String generateDailyContractNumber() {
        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long countToday = documentRepository.countCreatedBetween(startOfDay, endOfDay);
        long next = countToday + 1;

        // формат R-YYYY-MM-DD-001, R-2025-12-04-002 и т.п.
        return "R-" + today + "-" + String.format("%03d", next);
    }

    @Transactional
    public RentalDocumentDto createContract(CreateContractRequest req) {
        if (req.clientId() == null) {
            throw new BadRequestException("Не передан clientId");
        }
        if (req.toolId() == null) {
            throw new BadRequestException("Не передан toolId");
        }

        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        Tool tool = toolRepository.findById(req.toolId())
                .orElseThrow(() -> new NotFoundException("Инструмент не найден"));

        toolRentalGuard.ensureAvailableForRental(tool);

        LocalDateTime startDateTime = req.startDateTime() != null ? req.startDateTime() : LocalDateTime.now();
        String contractNumber = generateDailyContractNumber();

        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(contractNumber)
                .startDateTime(startDateTime)
                .expectedReturnDate(req.expectedReturnDate())
                .amount(req.totalAmount())
                .build();

        documentRepository.save(doc);

        tool.setContract(doc);
        toolRepository.save(tool);

        // Audit logging
        auditLogService.logCreate("Contract", doc.getId(), Map.of(
                "contractNumber", doc.getContractNumber(),
                "clientId", client.getId(),
                "toolId", tool.getId()
        ));

        return toDto(doc);
    }
    
    private RentalDocumentDto toDto(RentalDocument doc) {
        return new RentalDocumentDto(
                doc.getId(),
                doc.getContractNumber(),
                doc.getStartDateTime(),
                doc.getExpectedReturnDate(),
                doc.getAmount(),
                doc.getCreatedAt(),
                doc.getClient() != null ? doc.getClient().getId() : null,
                doc.getClosedAt(),
                doc.getTerminatedAt(),
                doc.getTerminationReason(),
                doc.getStatus(),
                doc.getComment()
        );
    }

    @Transactional
    public void closeContract(Long contractId) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Договор не найден"));

        if (doc.getClosedAt() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Договор уже завершён",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<Tool> tools = toolRepository.findByContractId(contractId);
        for (Tool tool : tools) {
            tool.setContract(null);
            toolRepository.save(tool);
        }

        doc.setClosedAt(LocalDateTime.now());
        documentRepository.save(doc);

        // Audit logging
        auditLogService.logContractClose(contractId, Map.of(
                "contractNumber", doc.getContractNumber(),
                "toolsCount", tools.size()
        ));
    }

    @Transactional
    public RentalDocumentDto update(Long id, UpdateContractRequest req) {
        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND
                ));

        if (doc.getClosedAt() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_CLOSED",
                    "Нельзя редактировать закрытый договор",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (req.expectedReturnDate() != null) {
            doc.setExpectedReturnDate(req.expectedReturnDate());
        }

        if (req.amount() != null) {
            doc.setAmount(req.amount());
        }

        if (req.comment() != null) {
            doc.setComment(req.comment());
        }

        documentRepository.save(doc);

        // Audit logging
        Map<String, Object> changes = new java.util.HashMap<>();
        if (req.expectedReturnDate() != null) changes.put("expectedReturnDate", req.expectedReturnDate());
        if (req.amount() != null) changes.put("amount", req.amount());
        if (req.comment() != null) changes.put("comment", "updated");
        auditLogService.logUpdate("Contract", id, changes);

        return toDto(doc);
    }

    @Transactional
    public void terminateContract(Long contractId, String reason) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND
                ));

        if (doc.getClosedAt() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Договор уже завершён",
                    HttpStatus.BAD_REQUEST
            );
        }

        // освобождаем инструменты
        List<Tool> tools = toolRepository.findByContractId(contractId);
        for (Tool tool : tools) {
            tool.setContract(null);
            toolRepository.save(tool);
        }

        doc.setTerminatedAt(LocalDateTime.now());
        String terminationReason = reason != null && !reason.isBlank()
                ? reason
                : "Расторгнут без указания причины";
        doc.setTerminationReason(terminationReason);

        documentRepository.save(doc);

        // Audit logging
        auditLogService.logContractTerminate(contractId, terminationReason, Map.of(
                "contractNumber", doc.getContractNumber(),
                "toolsCount", tools.size()
        ));
    }

    @Transactional
    public byte[] generateContractFileAndMarkClient(ContractRequest request) throws IOException {
        contractValidator.validate(request);

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        return contractExcelService.generate(client, request);
    }

    @Transactional(readOnly = true)
    public List<RentalDocumentDto> getAll() {
        return documentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalDocumentDto getById(Long id) {
        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND
                ));
        return toDto(doc);
    }
}


