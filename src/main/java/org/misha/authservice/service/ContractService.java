package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractRowDto;
import org.misha.authservice.dto.CloseContractRequest;
import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.dto.ContractTableDto;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.RentalDocumentDto;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ContractStatus;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.exception.NotFoundException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractCrudService crudService;
    private final ContractReportService reportService;
    private final ContractExcelService contractExcelService;
    private final ContractValidator contractValidator;
    private final ClientRepository clientRepository;

    @Transactional
    public RentalDocumentDto createContract(CreateContractRequest req) {
        return crudService.createContract(req);
    }

    @Transactional
    public void closeContract(Long contractId, CloseContractRequest req) {
        crudService.closeContract(contractId, req);
    }

    @Transactional
    public RentalDocumentDto update(Long id, UpdateContractRequest req) {
        return crudService.update(id, req);
    }

    @Transactional
    public void restoreContract(Long contractId) {
        crudService.restoreContract(contractId);
    }

    @Transactional
    public byte[] generateContractFileAndMarkClient(ContractRequest request) throws IOException {
        contractValidator.validate(request);

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));

        return contractExcelService.generate(client, request);
    }

    @Transactional(readOnly = true)
    public byte[] generateContractExcelById(Long contractId) {
        return contractExcelService.generateById(contractId);
    }

    @Transactional(readOnly = true)
    public Page<RentalDocumentDto> getAll(int page, int size) {
        return crudService.getAll(page, size);
    }

    @Transactional(readOnly = true)
    public RentalDocumentDto getById(Long id) {
        return crudService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<ActiveContractRowDto> getActiveContractsTable() {
        return reportService.getActiveContractsTable();
    }

    @Transactional(readOnly = true)
    public List<ContractTableDto> getHistoryTable(Long clientId, Long toolId,
            LocalDate from, LocalDate to, ContractStatus status) {
        return reportService.getHistoryTable(clientId, toolId, from, to, status);
    }
}
