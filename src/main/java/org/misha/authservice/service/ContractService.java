package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.TerminateContractRequest;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final RentalDocumentRepository documentRepository;
    private final ContractExcelService contractExcelService;
    private final ContractValidator contractValidator;

    @Transactional
    public RentalDocumentDto createContract(CreateContractRequest req) {
        if (req.clientId() == null) {
            throw new BadRequestException("Не передан clientId");
        }
        if (req.toolId() == null) {
            throw new BadRequestException("Не передан toolId");
        }
        if (req.contractNumber() == null || req.contractNumber().isBlank()) {
            throw new BadRequestException("Не передан номер договора");
        }

        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        Tool tool = toolRepository.findById(req.toolId())
                .orElseThrow(() -> new NotFoundException("Инструмент не найден"));

        if (tool.getContract() != null) {
            throw new BadRequestException("Инструмент занят");
        }

        LocalDateTime startDateTime = req.startDateTime() != null ? req.startDateTime() : LocalDateTime.now();

        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(req.contractNumber())
                .startDateTime(startDateTime)
                .expectedReturnDate(req.expectedReturnDate())
                .amount(req.totalAmount())
                .build();

        documentRepository.save(doc);

        tool.setContract(doc);
        toolRepository.save(tool);

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
                doc.getStatus()
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
        doc.setTerminationReason(
                reason != null && !reason.isBlank()
                        ? reason
                        : "Расторгнут без указания причины"
        );

        documentRepository.save(doc);
    }

    @Transactional
    public byte[] generateContractFileAndMarkClient(ContractRequest request) throws IOException {
        contractValidator.validate(request);

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        return contractExcelService.generate(client, request);
    }
}


