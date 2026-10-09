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
    private final RentalWriteLock writeLock;
    private final RentalDocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolTemplateRepository templateRepository;
    private final ClientMapper clientMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public ContractSyncDto.SyncResponse syncContracts(ContractSyncDto syncDto) {
        writeLock.acquire();
        List<ContractSyncDto.IdMapping> idMappings = new ArrayList<>();

        // 1. Process creations
        if (syncDto.getCreations() != null) {
            for (ContractSyncDto.CreateItem item : syncDto.getCreations()) {
                CreateContractRequest req =     new CreateContractRequest(
                        item.getClientId(),
                        item.getToolId(),
                        item.getToolIds(),
                        item.getContractNumber(),
                        item.getOfflineId(), item.getStartDateTime());
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

                if (id == null) throw new org.misha.authservice.exception.NotFoundException("Offline contract has not been created yet");
                if (id != null) {
                    checkRevision(id, item.getExpectedUpdatedAt());
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

                if (id == null) throw new org.misha.authservice.exception.NotFoundException("Offline contract has not been created yet");
                if (id != null) {
                    checkRevision(id, item.getExpectedUpdatedAt());
                    contractService.closeContract(id,
                            new CloseContractRequest(item.getPaidAmount(), item.getComment(), item.isBroken(), item.getActualReturnDate()));
                }
            }
        }

        documentRepository.flush();
        var changedIds = new java.util.LinkedHashSet<Long>();
        idMappings.forEach(m -> changedIds.add(m.getBackendId()));
        if (syncDto.getUpdates() != null) syncDto.getUpdates().forEach(i -> changedIds.add(resolveId(i.getId(), i.getOfflineId())));
        if (syncDto.getClosures() != null) syncDto.getClosures().forEach(i -> changedIds.add(resolveId(i.getId(), i.getOfflineId())));
        idMappings.clear();
        for (Long changedId : changedIds) {
            var d = documentRepository.findById(changedId).orElseThrow();
            var mapping = new ContractSyncDto.IdMapping(d.getOfflineId(), d.getId(), d.getContractNumber());
            mapping.setUpdatedAt(d.getUpdatedAt());
            idMappings.add(mapping);
        }
        return ContractSyncDto.SyncResponse.builder()
                .idMappings(idMappings)
                .build();
    }

    private Long resolveId(Long id, String offlineId) {
        return id != null ? id : documentRepository.findByOfflineId(offlineId).orElseThrow().getId();
    }
    private void checkRevision(Long id, LocalDateTime expected) {
        var doc = documentRepository.findById(id).orElseThrow(() -> new org.misha.authservice.exception.NotFoundException("Договор не найден"));
        if (expected != null && !expected.equals(doc.getUpdatedAt()))
            throw new org.misha.authservice.exception.AppException("SYNC_CONFLICT", "Договор изменён другим пользователем. Проверьте актуальные данные перед повторной отправкой.", org.springframework.http.HttpStatus.CONFLICT);
    }
    /** Full snapshot deliberately avoids the unsafe updatedAt watermark protocol. */
    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public SyncPullResponse pullSync(Instant sinceMillis, Long branchId) {
        return SyncPullResponse.builder()
            .clients(clientRepository.findAll().stream().map(clientMapper::toDtoForDetail).toList())
            .tools(ToolInstanceRepository.findAllWithTemplate().stream().map(ToolDto::fromEntity).toList())
            .categories(categoryRepository.findAll().stream().map(c -> new CategoryDto(c.getId(), c.getName())).toList())
            .templates(templateRepository.findAll().stream().map(t -> new org.misha.authservice.dto.TemplateFullDto(
                t.getId(), t.getName(), t.getCategory() == null ? null : t.getCategory().getId(),
                t.getDailyRentalPrice(), t.getDepositAmount(), t.getPurchasePrice(), List.of())).toList())
            .documents(documentRepository.findAll().stream().map(this::toDto).toList())
            .deletedClientIds(List.of()).deletedToolIds(List.of()).deletedCategoryIds(List.of())
            .deletedTemplateIds(List.of()).deletedDocumentIds(List.of())
            .fullSnapshot(true).fullSyncRequired(false).serverTimestamp(Instant.now()).build();
    }

    private RentalDocumentDto toDto(org.misha.authservice.entity.RentalDocument doc) {
        return RentalDocumentDto.fromEntity(doc);
    }
}

