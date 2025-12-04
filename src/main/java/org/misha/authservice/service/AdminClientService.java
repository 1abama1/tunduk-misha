package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.*;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.Tag;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.mapper.ClientMapper;
import org.misha.authservice.repository.ClientImageRepository;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminClientService {

    private final ClientRepository clientRepository;
    private final RentalDocumentRepository documentRepository;
    private final ClientImageRepository imageRepository;
    private final ClientMapper clientMapper;

    public List<ClientSearchResultDto> advancedSearch(
            String query,
            String tags,
            Boolean hasDocuments,
            Integer minDocs,
            Integer maxDocs,
            Boolean simpleMode, // У клиентов simpleMode нет → игнорируем
            Boolean consentPersonalData, // У клиентов нет — игнорируем
            String contractNumber,
            String sort,
            String direction
    ) {

        List<Client> list = clientRepository.findAll();

        // текстовый поиск
        if (StringUtils.hasText(query)) {
            String q = query.toLowerCase();
            list = list.stream().filter(c ->
                    (c.getFullName() != null && c.getFullName().toLowerCase().contains(q)) ||
                    (c.getPhone() != null && c.getPhone().toLowerCase().contains(q)) ||
                    (c.getAddress() != null && c.getAddress().toLowerCase().contains(q)) ||
                    (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        // поиск по тегу
        if (StringUtils.hasText(tags)) {
            Tag requiredTag = Tag.valueOf(tags.trim());
            list = list.stream()
                    .filter(c -> c.getTag() != null && c.getTag().equals(requiredTag))
                    .collect(Collectors.toList());
        }

        // поиск по документам
        if (hasDocuments != null) {
            if (hasDocuments) {
                list = list.stream()
                        .filter(c -> c.getDocuments() != null && !c.getDocuments().isEmpty())
                        .collect(Collectors.toList());
            } else {
                list = list.stream()
                        .filter(c -> c.getDocuments() == null || c.getDocuments().isEmpty())
                        .collect(Collectors.toList());
            }
        }

        // поиск по номеру контракта
        if (StringUtils.hasText(contractNumber)) {
            list = list.stream()
                    .filter(c -> c.getDocuments() != null && c.getDocuments().stream()
                            .anyMatch(d -> d.getContractNumber() != null && 
                                    d.getContractNumber().contains(contractNumber)))
                    .toList();
        }

        // фильтр по количеству документов
        if (minDocs != null) {
            list = list.stream()
                    .filter(c -> c.getDocuments() != null && c.getDocuments().size() >= minDocs)
                    .collect(Collectors.toList());
        }
        if (maxDocs != null) {
            list = list.stream()
                    .filter(c -> c.getDocuments() != null && c.getDocuments().size() <= maxDocs)
                    .collect(Collectors.toList());
        }

        // сортировка
        Comparator<Client> cmp = switch (sort) {
            case "name" -> Comparator.comparing(Client::getFullName, Comparator.nullsLast(String::compareTo));
            case "email" -> Comparator.comparing(Client::getEmail, Comparator.nullsLast(String::compareTo));
            case "documents" -> Comparator.comparingInt(c -> c.getDocuments() != null ? c.getDocuments().size() : 0);
            default -> Comparator.comparing(Client::getId);
        };

        if ("desc".equalsIgnoreCase(direction))
            cmp = cmp.reversed();

        list = list.stream().sorted(cmp).toList();

        // DTO
        return list.stream()
                .map(c -> new ClientSearchResultDto(
                        c.getId(),
                        defaultString(c.getFullName()),
                        defaultString(c.getPhone()),
                        defaultString(c.getEmail()),
                        defaultString(c.getAddress()),
                        c.getTag() != null ? c.getTag().name() : null,
                        c.getDocuments() != null ? c.getDocuments().size() : 0
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientDto getClientFull(Long clientId) {
        Client client = clientRepository.findByIdWithDocuments(clientId)
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));
        
        // Fetch tools for documents separately to avoid MultipleBagFetchException
        if (!client.getDocuments().isEmpty()) {
            List<Long> documentIds = client.getDocuments().stream()
                    .map(org.misha.authservice.entity.RentalDocument::getId)
                    .toList();
            documentRepository.findByIdsWithTools(documentIds);
        }

        ClientDto dto = clientMapper.toDto(client);
        dto.setImages(
                imageRepository.findByClientId(clientId).stream()
                        .map(img -> new ClientImageDto(
                                img.getId(),
                                img.getFileName(),
                                img.getFileType()
                        ))
                        .toList()
        );
        return dto;
    }

    @Transactional(readOnly = true)
    public List<RentalDocumentDto> getClientDocuments(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND);
        }

        return documentRepository.findByClientId(clientId)
                .stream()
                .map(doc -> new RentalDocumentDto(
                        doc.getId(),
                        doc.getContractNumber(),
                        doc.getStartDateTime(),
                        doc.getExpectedReturnDate(),
                        doc.getAmount(),
                        doc.getCreatedAt(),
                        doc.getClient().getId(),
                        doc.getClosedAt(),
                        doc.getTerminatedAt(),
                        doc.getTerminationReason(),
                        doc.getStatus(),
                        doc.getComment()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalDocumentDto> getActiveContracts(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND);
        }

        return documentRepository.findActiveContractsByClientId(clientId)
                .stream()
                .map(doc -> new RentalDocumentDto(
                        doc.getId(),
                        doc.getContractNumber(),
                        doc.getStartDateTime(),
                        doc.getExpectedReturnDate(),
                        doc.getAmount(),
                        doc.getCreatedAt(),
                        doc.getClient().getId(),
                        doc.getClosedAt(),
                        doc.getTerminatedAt(),
                        doc.getTerminationReason(),
                        doc.getStatus(),
                        doc.getComment()
                ))
                .toList();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}

