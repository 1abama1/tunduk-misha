package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CloseContractRequest;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.exception.NotFoundException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContractCrudService {

    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final RentalDocumentRepository documentRepository;
    private final ToolRentalGuard toolRentalGuard;
    private final AuditLogService auditLogService;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_CONTRACT_NUMBER_ATTEMPTS = 100;
    private final java.security.SecureRandom random = new java.security.SecureRandom();

    private String generateRandomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String generateDailyContractNumber() {
        LocalDate today = LocalDate.now();
        String prefix = "R-" + today + "-";
        String contractNumber;
        int attempts = 0;

        do {
            contractNumber = prefix + generateRandomSuffix(3);
            attempts++;
            if (attempts > MAX_CONTRACT_NUMBER_ATTEMPTS) {
                contractNumber = prefix + generateRandomSuffix(6);
            }
        } while (documentRepository.existsByContractNumber(contractNumber));

        return contractNumber;
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
        String contractNumber = (req.contractNumber() != null && !req.contractNumber().isBlank())
                ? req.contractNumber()
                : generateDailyContractNumber();

        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(contractNumber)
                .startDateTime(startDateTime)
                .amount(null)
                .dailyPrice(null)
                .build();

        documentRepository.save(doc);

        tool.setContract(doc);
        toolRepository.save(tool);

        doc.setToolId(tool.getId());
        documentRepository.save(doc);

        auditLogService.logCreate("Contract", doc.getId(), Map.of(
                "contractNumber", doc.getContractNumber(),
                "clientId", client.getId(),
                "toolId", tool.getId()));

        return toDto(doc);
    }

    @Transactional
    public void closeContract(Long contractId, CloseContractRequest req) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Договор не найден"));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException("CONTRACT_ALREADY_CLOSED", "Договор уже завершён", HttpStatus.BAD_REQUEST);
        }

        List<Tool> tools = toolRepository.findByContractId(contractId);

        if (!tools.isEmpty()) {
            doc.setToolId(tools.get(0).getId());
        }

        for (Tool tool : tools) {
            tool.setContract(null);
            toolRepository.save(tool);
        }

        doc.setReturnDate(LocalDateTime.now());
        if (req != null) {
            if (req.paidAmount() != null) {
                doc.setAmount(req.paidAmount());
            }
            if (req.comment() != null) {
                doc.setComment(req.comment());
            }
        }

        documentRepository.save(doc);

        auditLogService.logContractClose(contractId, Map.of(
                "contractNumber", doc.getContractNumber(),
                "toolsCount", tools.size(),
                "paidAmount", req != null ? req.paidAmount() : "N/A"));
    }

    @Transactional
    public RentalDocumentDto update(Long id, UpdateContractRequest req) {
        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("CONTRACT_NOT_FOUND", "Договор не найден", HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException("CONTRACT_CLOSED", "Нельзя редактировать закрытый договор", HttpStatus.BAD_REQUEST);
        }

        if (req.comment() != null) {
            doc.setComment(req.comment());
        }

        documentRepository.save(doc);

        Map<String, Object> changes = new java.util.HashMap<>();
        if (req.comment() != null)
            changes.put("comment", "updated");
        auditLogService.logUpdate("Contract", id, changes);

        return toDto(doc);
    }

    @Transactional
    public void restoreContract(Long contractId) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException("CONTRACT_NOT_FOUND", "Договор не найден", HttpStatus.NOT_FOUND));

        if (doc.getTerminatedAt() != null) {
            throw new AppException("CONTRACT_TERMINATED", "Расторгнутый договор нельзя восстановить", HttpStatus.BAD_REQUEST);
        }

        if (doc.getReturnDate() == null) {
            throw new AppException("CONTRACT_ALREADY_ACTIVE", "Договор уже активен", HttpStatus.BAD_REQUEST);
        }

        if (doc.getToolId() == null) {
            throw new AppException("TOOL_ID_MISSING", "Невозможно восстановить договор — не найден инструмент", HttpStatus.BAD_REQUEST);
        }

        Tool tool = toolRepository.findById(doc.getToolId())
                .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Инструмент не найден", HttpStatus.NOT_FOUND));

        if (tool.getContract() != null && !Objects.equals(tool.getContract().getId(), doc.getId())) {
            throw new AppException("TOOL_BUSY", "Инструмент уже используется в другом договоре", HttpStatus.CONFLICT);
        }

        tool.setContract(doc);
        toolRepository.save(tool);

        doc.setReturnDate(null);
        doc.setTerminatedAt(null);
        doc.setTerminationReason(null);
        documentRepository.save(doc);

        auditLogService.logUpdate("Contract", contractId, Map.of(
                "action", "RESTORE",
                "toolId", tool.getId()));
    }

    @Transactional(readOnly = true)
    public Page<RentalDocumentDto> getAll(int page, int size) {
        Page<RentalDocument> docsPage = documentRepository.findAll(PageRequest.of(page, size));
        return docsPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public RentalDocumentDto getById(Long id) {
        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("CONTRACT_NOT_FOUND", "Договор не найден", HttpStatus.NOT_FOUND));
        return toDto(doc);
    }

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
