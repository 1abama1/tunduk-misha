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

@Service
@RequiredArgsConstructor
public class AdminClientService {

    private final ClientRepository clientRepository;
    private final RentalDocumentRepository documentRepository;
    private final ClientImageRepository imageRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
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
            String direction) {
        // SQL-фильтрация на уровне базы данных
        Tag tag = null;
        if (StringUtils.hasText(tags)) {
            try {
                tag = Tag.valueOf(tags.trim());
            } catch (IllegalArgumentException e) {
                // Игнорируем неверный тег
            }
        }

        List<Client> list = clientRepository.searchAdvanced(
                StringUtils.hasText(query) ? query : null,
                tag,
                hasDocuments,
                StringUtils.hasText(contractNumber) ? contractNumber : null,
                minDocs,
                maxDocs);

        // Сортировка (можно также вынести в SQL, но для простоты оставляем в памяти)
        Comparator<Client> cmp = switch (sort) {
            case "name" -> Comparator.comparing(Client::getFullName, Comparator.nullsLast(String::compareTo));
            case "email" -> Comparator.comparing(Client::getEmail, Comparator.nullsLast(String::compareTo));
            case "documents" -> Comparator.comparingInt(c -> {
                // Подсчет документов - можно оптимизировать через JOIN COUNT в SQL
                return c.getDocuments() != null ? c.getDocuments().size() : 0;
            });
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
                        addressToString(c.getRegistrationAddress()),
                        addressToString(c.getLivingAddress()),
                        c.getTag() != null ? c.getTag().name() : null,
                        c.getDocuments() != null ? c.getDocuments().size() : 0))
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
                                img.getFileType()))
                        .toList());
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
                        doc.getDailyPrice(),
                        doc.getAmount(),
                        doc.getCreatedAt(),
                        doc.getClient().getId(),
                        doc.getReturnDate(),
                        doc.getTerminatedAt(),
                        doc.getTerminationReason(),
                        doc.getStatus(),
                        doc.getComment()))
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
                        doc.getDailyPrice(),
                        doc.getAmount(),
                        doc.getCreatedAt(),
                        doc.getClient().getId(),
                        doc.getReturnDate(),
                        doc.getTerminatedAt(),
                        doc.getTerminationReason(),
                        doc.getStatus(),
                        doc.getComment()))
                .toList();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String addressToString(org.misha.authservice.entity.Address address) {
        if (address == null)
            return "";
        String region = address.getRegion() != null ? address.getRegion() : "";
        String street = address.getStreet() != null ? address.getStreet() : "";
        if (region.isBlank() && street.isBlank())
            return "";
        if (street.isBlank())
            return region;
        if (region.isBlank())
            return street;
        return region + ", " + street;
    }
}
