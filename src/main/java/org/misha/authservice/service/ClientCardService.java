package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.ActiveContractDto;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.ClientCardDto;
import org.misha.authservice.dto.ClientImageDto;
import org.misha.authservice.dto.ContractHistoryDto;
import org.misha.authservice.entity.Address;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientImageRepository;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientCardService {

    private final ClientRepository clientRepository;
    private final RentalDocumentRepository documentRepository;
    private final ClientImageRepository imageRepository;

    @Transactional(readOnly = true)
    public ClientCardDto getClientCard(Long clientId) {
        var client = clientRepository.findByIdWithDocuments(clientId)
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));

        if (!client.getDocuments().isEmpty()) {
            List<Long> docIds = client.getDocuments()
                    .stream()
                    .map(RentalDocument::getId)
                    .toList();
            if (!docIds.isEmpty()) {
                List<RentalDocument> loaded = documentRepository.findByIdsWithTools(docIds);
                Map<Long, RentalDocument> loadedById = loaded.stream()
                        .collect(Collectors.toMap(RentalDocument::getId, d -> d));
                List<RentalDocument> resolved = client.getDocuments().stream()
                        .map(doc -> loadedById.getOrDefault(doc.getId(), doc))
                        .collect(Collectors.toCollection(ArrayList::new));
                client.setDocuments(resolved);
            }
        }

        List<ActiveContractDto> active = client.getDocuments().stream()
                .filter(d -> d.getClosedAt() == null && d.getTerminatedAt() == null)
                .map(this::toActiveContract)
                .toList();

        List<ContractHistoryDto> history = client.getDocuments().stream()
                .filter(d -> d.getClosedAt() != null || d.getTerminatedAt() != null)
                .map(this::toHistory)
                .toList();

        return ClientCardDto.builder()
                .id(client.getId())
                .fullName(client.getFullName())
                .phone(client.getPhone())
                .registrationAddress(toAddressDto(client.getRegistrationAddress()))
                .livingAddress(toAddressDto(client.getLivingAddress()))
                .email(client.getEmail())
                .tag(client.getTag() != null ? client.getTag().name() : null)
                .activeContracts(active)
                .history(history)
                .images(imageRepository.findByClientId(clientId).stream()
                        .map(img -> new ClientImageDto(
                                img.getId(),
                                img.getFileName(),
                                img.getFileType()
                        ))
                        .toList())
                .build();
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) return null;
        return new AddressDto(address.getRegion(), address.getStreet());
    }

    private ActiveContractDto toActiveContract(RentalDocument doc) {
        Tool tool = doc.getTools().isEmpty() ? null : doc.getTools().get(0);
        return ActiveContractDto.builder()
                .id(doc.getId())
                .contractNumber(doc.getContractNumber())
                .startDateTime(doc.getStartDateTime())
                .toolName(tool != null && tool.getTemplate() != null ? tool.getTemplate().getName() : null)
                .serialNumber(tool != null ? tool.getSerialNumber() : null)
                .build();
    }

    private ContractHistoryDto toHistory(RentalDocument doc) {
        Tool tool = doc.getTools().isEmpty() ? null : doc.getTools().get(0);
        return ContractHistoryDto.builder()
                .id(doc.getId())
                .contractNumber(doc.getContractNumber())
                .startDateTime(doc.getStartDateTime())
                .closedAt(doc.getClosedAt())
                .terminatedAt(doc.getTerminatedAt())
                .terminationReason(doc.getTerminationReason())
                .toolName(tool != null && tool.getTemplate() != null ? tool.getTemplate().getName() : null)
                .status(doc.getStatus())
                .build();
    }
}

