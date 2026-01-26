package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CloseContractRequest;
import org.misha.authservice.dto.ContractSyncDto;
import org.misha.authservice.dto.CreateContractRequest;
import org.misha.authservice.dto.UpdateContractRequest;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final ContractService contractService;
    private final RentalDocumentRepository documentRepository;

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
                        item.getContractNumber(),
                        item.getStartDateTime());
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
                            new CloseContractRequest(item.getPaidAmount(), item.getComment()));
                }
            }
        }

        return ContractSyncDto.SyncResponse.builder()
                .idMappings(idMappings)
                .build();
    }
}
