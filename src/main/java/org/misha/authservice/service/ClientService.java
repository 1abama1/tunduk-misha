package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.AddressDto;
import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.ClientImageDto;
import org.misha.authservice.dto.ClientLightSearchDto;
import org.misha.authservice.dto.CreateClientRequest;
import org.misha.authservice.dto.PassportDto;
import org.misha.authservice.dto.UpdateClientRequest;
import org.misha.authservice.entity.Address;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientPassport;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.mapper.ClientMapper;
import org.misha.authservice.repository.ClientImageRepository;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final RentalDocumentRepository rentalDocumentRepository;
    private final ClientImageRepository imageRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ClientDto create(CreateClientRequest req) {

        if (clientRepository.existsByPhone(req.phone()))
            throw new AppException("PHONE_EXISTS", "Телефон уже используется!", HttpStatus.CONFLICT);

        if (req.email() != null && clientRepository.existsByEmail(req.email()))
            throw new AppException("EMAIL_EXISTS", "Email уже используется!", HttpStatus.CONFLICT);

        Client client = Client.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .whatsappPhone(req.whatsappPhone())
                .registrationAddress(toAddress(req.registrationAddress()))
                .livingAddress(toAddress(req.livingAddress()))
                .objectAddress(req.objectAddress())
                .email(req.email())
                .birthDate(req.birthDate())
                .comment(req.comment())
                .tag(req.tag())
                .build();

        applyPassport(client, req.passport());

        clientRepository.save(client);

        ClientDto dto = clientMapper.toDto(client);
        dto.setImages(
                imageRepository.findByClientId(client.getId()).stream()
                        .map(img -> new ClientImageDto(
                                img.getId(),
                                img.getFileName(),
                                img.getFileType()))
                        .toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public ClientDto get(Long id) {
        Client client = clientRepository.findByIdWithDocuments(id)
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND));

        // Fetch tools for documents separately to avoid MultipleBagFetchException
        if (!client.getDocuments().isEmpty()) {
            List<Long> documentIds = client.getDocuments().stream()
                    .map(RentalDocument::getId)
                    .toList();
            rentalDocumentRepository.findByIdsWithTools(documentIds);
        }

        return clientMapper.toDto(client);
    }

    @Transactional(readOnly = true)
    public List<ClientDto> getAll() {
        // First fetch clients with documents
        List<Client> clients = clientRepository.findAllWithDocuments();

        // Then fetch tools for all documents to avoid MultipleBagFetchException
        if (!clients.isEmpty()) {
            List<Long> documentIds = clients.stream()
                    .flatMap(client -> client.getDocuments().stream())
                    .map(RentalDocument::getId)
                    .toList();
            if (!documentIds.isEmpty()) {
                rentalDocumentRepository.findByIdsWithTools(documentIds);
            }
        }

        return clients.stream()
                .map(client -> {
                    ClientDto dto = clientMapper.toDto(client);
                    dto.setImages(
                            imageRepository.findByClientId(client.getId()).stream()
                                    .map(img -> new ClientImageDto(
                                            img.getId(),
                                            img.getFileName(),
                                            img.getFileType()))
                                    .toList());
                    return dto;
                })
                .toList();
    }

    @Transactional
    public ClientDto update(Long id, UpdateClientRequest req) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND));

        if (req.fullName() != null)
            client.setFullName(req.fullName());
        if (req.phone() != null)
            client.setPhone(req.phone());
        if (req.whatsappPhone() != null)
            client.setWhatsappPhone(req.whatsappPhone());
        if (req.registrationAddress() != null)
            client.setRegistrationAddress(toAddress(req.registrationAddress()));
        if (req.livingAddress() != null)
            client.setLivingAddress(toAddress(req.livingAddress()));
        if (req.objectAddress() != null)
            client.setObjectAddress(req.objectAddress());
        if (req.email() != null)
            client.setEmail(req.email());
        if (req.birthDate() != null)
            client.setBirthDate(req.birthDate());
        if (req.comment() != null)
            client.setComment(req.comment());
        if (req.tag() != null)
            client.setTag(req.tag());

        applyPassport(client, req.passport());

        clientRepository.save(client);

        // Перезагружаем клиента с документами
        Client updatedClient = clientRepository.findByIdWithDocuments(id)
                .orElse(client);

        // Fetch tools for documents separately to avoid MultipleBagFetchException
        if (!updatedClient.getDocuments().isEmpty()) {
            List<Long> documentIds = updatedClient.getDocuments().stream()
                    .map(RentalDocument::getId)
                    .toList();
            rentalDocumentRepository.findByIdsWithTools(documentIds);
        }

        return clientMapper.toDto(updatedClient);
    }

    public void delete(Long id) {
        if (!clientRepository.existsById(id))
            throw new AppException("CLIENT_NOT_FOUND", "Клиент не найден", HttpStatus.NOT_FOUND);
        clientRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ClientLightSearchDto> search(String query) {
        String normalized = (query == null || query.isBlank()) ? null : query.trim();
        return clientRepository.searchLight(normalized);
    }

    private void applyPassport(Client client, PassportDto dto) {
        if (dto == null) {
            return;
        }
        ClientPassport passport = client.getPassport();
        if (passport == null) {
            passport = new ClientPassport();
            passport.setClient(client);
        }
        passport.setSeries(dto.series());
        passport.setNumber(dto.number());
        passport.setIssuedBy(dto.issuedBy());
        passport.setSubdivisionCode(dto.subdivisionCode());
        passport.setIssueDate(dto.issueDate());
        passport.setInn(dto.inn());
        client.setPassport(passport);
    }

    private Address toAddress(AddressDto dto) {
        if (dto == null)
            return null;
        return new Address(dto.region(), dto.street());
    }
}
