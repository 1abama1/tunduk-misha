package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.ContractTableDto;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.TerminateContractRequest;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ContractStatus;
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
import java.util.Objects;

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
        
        // Сохраняем toolId в документе
        doc.setToolId(tool.getId());
        documentRepository.save(doc);

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
                doc.getDailyPrice(),
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
        
        // Сохраняем toolId перед отвязкой инструментов
        if (!tools.isEmpty()) {
            doc.setToolId(tools.get(0).getId());
        }
        
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
        
        // Сохраняем toolId перед отвязкой инструментов
        if (!tools.isEmpty()) {
            doc.setToolId(tools.get(0).getId());
        }
        
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

    /**
     * Получает список активных договоров в формате таблицы.
     * Возвращает все активные (не закрытые и не расторгнутые) договоры с данными клиента и инструмента.
     */
    @Transactional(readOnly = true)
    public List<ActiveContractRowDto> getActiveContractsTable() {
        List<RentalDocument> list = documentRepository.findAllActive();

        return list.stream().map(c -> {
            // Получаем имя клиента
            String clientName = c.getClient() != null && c.getClient().getFullName() != null
                    ? c.getClient().getFullName()
                    : "—";

            // Получаем инструменты с template (JOIN FETCH)
            List<Tool> tools = toolRepository.findByContractIdWithTemplate(c.getId());
            
            // Формируем имя инструмента
            String toolName;
            if (tools.isEmpty()) {
                toolName = "—";
            } else if (tools.size() == 1) {
                Tool tool = tools.get(0);
                // Используем tool.getName() если есть, иначе template.getName()
                if (tool.getName() != null && !tool.getName().isBlank()) {
                    toolName = tool.getName();
                } else if (tool.getTemplate() != null && tool.getTemplate().getName() != null) {
                    toolName = tool.getTemplate().getName();
                } else {
                    toolName = "—";
                }
            } else {
                // Если несколько инструментов, объединяем их имена
                toolName = tools.stream()
                        .map(t -> {
                            if (t.getName() != null && !t.getName().isBlank()) {
                                return t.getName();
                            } else if (t.getTemplate() != null && t.getTemplate().getName() != null) {
                                return t.getTemplate().getName();
                            } else {
                                return "—";
                            }
                        })
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("—");
            }

            // Форматируем дату начала
            String startDate = c.getStartDateTime() != null
                    ? c.getStartDateTime().toString()
                    : "—";

            // Баланс = amount
            Double balance = c.getAmount() != null ? c.getAmount() : 0.0;

            return ActiveContractRowDto.builder()
                    .id(c.getId())
                    .clientName(clientName)
                    .toolName(toolName)
                    .startDate(startDate)
                    .balance(balance)
                    .build();
        }).toList();
    }

    /**
     * История договоров (ACTIVE, CLOSED, TERMINATED) с сортировкой по startDateTime DESC.
     * Фильтры: clientId, toolId, from, to, status (статус фильтруем в сервисе, так как он вычисляемый).
     */
    @Transactional(readOnly = true)
    public List<ContractTableDto> getHistoryTable(Long clientId,
                                                  Long toolId,
                                                  LocalDate from,
                                                  LocalDate to,
                                                  ContractStatus status) {
        LocalDateTime fromDate = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDate = to != null ? to.plusDays(1).atStartOfDay() : null;

        List<RentalDocument> docs;
        if (fromDate != null && toDate != null) {
            docs = documentRepository.findHistoryBetween(clientId, toolId, fromDate, toDate);
        } else if (fromDate != null) {
            docs = documentRepository.findHistoryFrom(clientId, toolId, fromDate);
        } else if (toDate != null) {
            docs = documentRepository.findHistoryTo(clientId, toolId, toDate);
        } else {
            docs = documentRepository.findHistoryWithoutDate(clientId, toolId);
        }

        return docs.stream()
                .map(doc -> toTableDto(doc, status))
                .filter(Objects::nonNull)
                .toList();
    }

    private ContractTableDto toTableDto(RentalDocument doc, ContractStatus statusFilter) {
        ContractStatus derivedStatus = doc.getStatus();
        if (statusFilter != null && derivedStatus != statusFilter) {
            return null;
        }

        // Получаем инструмент (берём первый, если их несколько)
        Tool tool = doc.getTools().isEmpty() ? null : doc.getTools().get(0);

        String toolName;
        String serialNumber;
        if (tool == null) {
            toolName = "—";
            serialNumber = null;
        } else {
            if (tool.getName() != null && !tool.getName().isBlank()) {
                toolName = tool.getName();
            } else if (tool.getTemplate() != null && tool.getTemplate().getName() != null) {
                toolName = tool.getTemplate().getName();
            } else {
                toolName = "—";
            }
            serialNumber = tool.getSerialNumber();
        }

        LocalDateTime actualReturn = doc.getClosedAt() != null
                ? doc.getClosedAt()
                : doc.getTerminatedAt();

        return ContractTableDto.builder()
                .id(doc.getId())
                .contractNumber(doc.getContractNumber())
                .clientName(doc.getClient() != null ? doc.getClient().getFullName() : "—")
                .toolName(toolName)
                .serialNumber(serialNumber)
                .startDateTime(doc.getStartDateTime())
                .expectedReturnDate(doc.getExpectedReturnDate())
                .actualReturnDate(actualReturn)
                .amount(doc.getAmount())
                .status(derivedStatus)
                .build();
    }
}


