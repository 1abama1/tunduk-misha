package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.ContractTableDto;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ContractStatus;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.BadRequestException;
import org.misha.authservice.exception.NotFoundException;
import org.misha.authservice.mapper.ExcelContractMapper;
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
    private final ExcelGeneratorService excelGeneratorService;
    private final ExcelContractMapper excelContractMapper;

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

        // Сохраняем toolId в документе
        doc.setToolId(tool.getId());
        documentRepository.save(doc);

        // Audit logging
        auditLogService.logCreate("Contract", doc.getId(), Map.of(
                "contractNumber", doc.getContractNumber(),
                "clientId", client.getId(),
                "toolId", tool.getId()));

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

    @Transactional
    public void closeContract(Long contractId) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Договор не найден"));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Договор уже завершён",
                    HttpStatus.BAD_REQUEST);
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

        doc.setReturnDate(LocalDateTime.now());
        documentRepository.save(doc);

        // Audit logging
        auditLogService.logContractClose(contractId, Map.of(
                "contractNumber", doc.getContractNumber(),
                "toolsCount", tools.size()));
    }

    @Transactional
    public RentalDocumentDto update(Long id, UpdateContractRequest req) {
        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_CLOSED",
                    "Нельзя редактировать закрытый договор",
                    HttpStatus.BAD_REQUEST);
        }

        if (req.comment() != null) {
            doc.setComment(req.comment());
        }

        documentRepository.save(doc);

        // Audit logging
        Map<String, Object> changes = new java.util.HashMap<>();
        if (req.comment() != null)
            changes.put("comment", "updated");
        auditLogService.logUpdate("Contract", id, changes);

        return toDto(doc);
    }

    @Transactional
    public void terminateContract(Long contractId, String reason) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Договор уже завершён",
                    HttpStatus.BAD_REQUEST);
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
                "toolsCount", tools.size()));
    }

    /**
     * Восстанавливает ранее закрытый договор (CLOSED) в активное состояние.
     * Не позволяет восстанавливать расторгнутые договоры (TERMINATED).
     */
    @Transactional
    public void restoreContract(Long contractId) {
        RentalDocument doc = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND));

        // Нельзя восстановить расторгнутый договор
        if (doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_TERMINATED",
                    "Расторгнутый договор нельзя восстановить",
                    HttpStatus.BAD_REQUEST);
        }

        // Если договор уже активен (нет returnDate), восстанавливать нечего
        if (doc.getReturnDate() == null) {
            throw new AppException(
                    "CONTRACT_ALREADY_ACTIVE",
                    "Договор уже активен",
                    HttpStatus.BAD_REQUEST);
        }

        // Для восстановления должен быть сохранён toolId
        if (doc.getToolId() == null) {
            throw new AppException(
                    "TOOL_ID_MISSING",
                    "Невозможно восстановить договор — не найден инструмент",
                    HttpStatus.BAD_REQUEST);
        }

        Tool tool = toolRepository.findById(doc.getToolId())
                .orElseThrow(() -> new AppException(
                        "TOOL_NOT_FOUND",
                        "Инструмент не найден",
                        HttpStatus.NOT_FOUND));

        // Инструмент не должен быть уже привязан к другому договору
        if (tool.getContract() != null && !Objects.equals(tool.getContract().getId(), doc.getId())) {
            throw new AppException(
                    "TOOL_BUSY",
                    "Инструмент уже используется в другом договоре",
                    HttpStatus.CONFLICT);
        }

        // Возвращаем связь инструмент ↔ договор
        tool.setContract(doc);
        toolRepository.save(tool);

        // Сбрасываем закрытие
        doc.setReturnDate(null);
        doc.setTerminatedAt(null);
        doc.setTerminationReason(null);
        documentRepository.save(doc);

        // Audit logging
        auditLogService.logUpdate("Contract", contractId, Map.of(
                "action", "RESTORE",
                "toolId", tool.getId()));
    }

    @Transactional
    public byte[] generateContractFileAndMarkClient(ContractRequest request) throws IOException {
        contractValidator.validate(request);

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        return contractExcelService.generate(client, request);
    }

    /**
     * Генерирует Excel файл договора по ID договора.
     * Использует новый ExcelGeneratorService с точным маппингом ячеек.
     * 
     * @param contractId ID договора
     * @return массив байтов готового .xlsx файла
     */
    @Transactional(readOnly = true)
    public byte[] generateContractExcelById(Long contractId) {
        // Получаем договор с клиентом
        RentalDocument document = documentRepository.findById(contractId)
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Договор не найден",
                        HttpStatus.NOT_FOUND));

        // Загружаем клиента с паспортом
        Client client = document.getClient();
        if (client == null) {
            throw new AppException(
                    "CLIENT_NOT_FOUND",
                    "Клиент не найден для договора",
                    HttpStatus.NOT_FOUND);
        }

        // Загружаем клиента с паспортом, если он еще не загружен
        if (client.getPassport() == null) {
            client = clientRepository.findByIdWithDocuments(client.getId())
                    .orElse(client);
        }

        // Получаем инструмент
        Tool tool = null;
        if (document.getToolId() != null) {
            tool = toolRepository.findByIdWithTemplateAndContract(document.getToolId())
                    .orElse(null);
        }

        // Если не нашли по toolId, пробуем найти по contractId
        if (tool == null) {
            List<Tool> tools = toolRepository.findByContractIdWithTemplate(contractId);
            if (!tools.isEmpty()) {
                tool = tools.get(0);
            }
        }

        // Преобразуем в DTO
        var excelDto = excelContractMapper.toExcelContractDto(document, tool, client);

        // Генерируем Excel
        return excelGeneratorService.generateContractExcel(excelDto);
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
                        HttpStatus.NOT_FOUND));
        return toDto(doc);
    }

    /**
     * Получает список активных договоров в формате таблицы.
     * Возвращает все активные (не закрытые и не расторгнутые) договоры с данными
     * клиента и инструмента.
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
     * История договоров (ACTIVE, CLOSED, TERMINATED) с сортировкой по startDateTime
     * DESC.
     * Фильтры: clientId, toolId, from, to, status (статус фильтруем в сервисе, так
     * как он вычисляемый).
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

        // Получаем инструмент по сохранённому toolId (doc.getTools() не используется)
        Tool tool = null;
        if (doc.getToolId() != null) {
            tool = toolRepository.findById(doc.getToolId()).orElse(null);
        }

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

        LocalDateTime actualReturn = doc.getReturnDate() != null
                ? doc.getReturnDate()
                : doc.getTerminatedAt();

        return ContractTableDto.builder()
                .id(doc.getId())
                .contractNumber(doc.getContractNumber())
                .clientName(doc.getClient() != null ? doc.getClient().getFullName() : "—")
                .toolName(toolName)
                .serialNumber(serialNumber)
                .startDateTime(doc.getStartDateTime())
                .returnDate(actualReturn)
                .amount(doc.getAmount())
                .status(derivedStatus)
                .build();
    }
}
