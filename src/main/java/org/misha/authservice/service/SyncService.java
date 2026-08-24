package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CloseContractRequest;
import org.misha.authservice.dto.ContractSyncDto;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.dto.SyncPullResponse;
import org.misha.authservice.mapper.ClientMapper;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.misha.authservice.dto.CategoryDto;
import org.misha.authservice.dto.TemplateDto;
import org.misha.authservice.dto.ToolDto;
import org.misha.authservice.dto.RentalDocumentDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final ContractService contractService;
    private final RentalDocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ContractSyncDto.SyncResponse syncContracts(ContractSyncDto syncDto) {
        List<ContractSyncDto.IdMapping> idMappings = new ArrayList<>();

        // 1. Process creations
        if (syncDto.getCreations() != null) {
            for (ContractSyncDto.CreateItem item : syncDto.getCreations()) {
                // Check if already synced by offlineId
                var existing = documentRepository.findByOfflineId(item.getOfflineId());
                if (existing.isPresent()) {
                    idMappings.add(new ContractSyncDto.IdMapping(item.getOfflineId(), existing.get().getId(),
                            existing.get().getContractNumber()));
                    continue;
                }

                CreateContractRequest req = new CreateContractRequest(
                        item.getClientId(),
                        item.getToolId(),
                        item.getToolIds(),
                        item.getContractNumber(),
                        item.getOfflineId());
                var created = contractService.createContract(req);

                // Update offlineId for the newly created contract
                var doc = documentRepository.findById(created.id()).orElseThrow();
                doc.setOfflineId(item.getOfflineId());
                documentRepository.save(doc);

                idMappings.add(
                        new ContractSyncDto.IdMapping(item.getOfflineId(), created.id(), created.contractNumber()));
            }
        }

        // 2. Process updates
        if (syncDto.getUpdates() != null) {
            for (ContractSyncDto.UpdateItem item : syncDto.getUpdates()) {
                Long id = item.getId();
                if (id == null && item.getOfflineId() != null) {
                    id = documentRepository.findByOfflineId(item.getOfflineId())
                            .map(org.misha.authservice.entity.RentalDocument::getId)
                            .orElse(null);
                }

                if (id != null) {
                    contractService.update(id, new UpdateContractRequest(item.getComment()));
                }
            }
        }

        // 3. Process closures
        if (syncDto.getClosures() != null) {
            for (ContractSyncDto.CloseItem item : syncDto.getClosures()) {
                Long id = item.getId();
                if (id == null && item.getOfflineId() != null) {
                    id = documentRepository.findByOfflineId(item.getOfflineId())
                            .map(org.misha.authservice.entity.RentalDocument::getId)
                            .orElse(null);
                }

                if (id != null) {
                    contractService.closeContract(id,
                            new CloseContractRequest(item.getPaidAmount(), item.getComment(), item.isBroken(), item.getActualReturnDate()));
                }
            }
        }

        return ContractSyncDto.SyncResponse.builder()
                .idMappings(idMappings)
                .build();
    }

    @Transactional(readOnly = true)
    public SyncPullResponse pullSync(long sinceMillis) {
        LocalDateTime since = LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceMillis), ZoneId.systemDefault());

        var clients = clientRepository.findByUpdatedAtAfter(since).stream()
                .map(clientMapper::toDto)
                .toList();

        var tools = ToolInstanceRepository.findByUpdatedAtAfter(since).stream()
                .map(ToolDto::fromEntity)
                .toList();

        var categories = categoryRepository.findByUpdatedAtAfter(since).stream()
                .map(c -> new CategoryDto(c.getId(), c.getName()))
                .toList();

        var templates = templateRepository.findByUpdatedAtAfter(since).stream()
                .map(t -> new TemplateDto(t.getId(), t.getName(), t.getCategory() != null ? t.getCategory().getId() : null))
                .toList();

        var documents = documentRepository.findByUpdatedAtAfter(since).stream()
                .map(this::toDto)
                .toList();

        return SyncPullResponse.builder()
                .clients(clients)
                .tools(tools)
                .categories(categories)
                .templates(templates)
                .documents(documents)
                .lastSyncTimestamp(System.currentTimeMillis())
                .build();
    }

    private RentalDocumentDto toDto(org.misha.authservice.entity.RentalDocument doc) {
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

