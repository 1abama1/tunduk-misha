package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.ContractTableDto;
import org.misha.authservice.entity.ContractStatus;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContractReportService {

    private final RentalDocumentRepository documentRepository;
    private final ToolRepository toolRepository;

    @Transactional(readOnly = true)
    public List<ActiveContractRowDto> getActiveContractsTable() {
        List<RentalDocument> list = documentRepository.findAllActive();

        return list.stream().map(c -> {
            String clientName = c.getClient() != null && c.getClient().getFullName() != null
                    ? c.getClient().getFullName()
                    : "—";

            List<Tool> tools = toolRepository.findByContractIdWithTemplate(c.getId());

            String toolName = "—";
            if (!tools.isEmpty()) {
                if (tools.size() == 1) {
                    toolName = getDisplayName(tools.get(0));
                } else {
                    toolName = tools.stream()
                            .map(this::getDisplayName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("—");
                }
            } else if (c.getToolId() != null) {
                toolName = toolRepository.findByIdWithTemplateAndContract(c.getToolId())
                        .map(this::getDisplayName)
                        .orElse("—");
            }

            String startDate = c.getStartDateTime() != null
                    ? c.getStartDateTime().toString()
                    : "—";

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

    private String getDisplayName(Tool tool) {
        if (tool.getName() != null && !tool.getName().isBlank()) {
            return tool.getName();
        } else if (tool.getTemplate() != null && tool.getTemplate().getName() != null) {
            return tool.getTemplate().getName();
        } else {
            return "—";
        }
    }

    private ContractTableDto toTableDto(RentalDocument doc, ContractStatus statusFilter) {
        ContractStatus derivedStatus = doc.getStatus();
        if (statusFilter != null && derivedStatus != statusFilter) {
            return null;
        }

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
