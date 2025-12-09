package org.misha.authservice.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.client.ClientCreateRequest;
import org.misha.authservice.dto.client.ClientResponseDto;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientTag;
import org.misha.authservice.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientDirectoryService {

    private final ClientRepository clientRepository;

    @Transactional
    public ClientResponseDto create(ClientCreateRequest dto) {
        Client client = Client.builder()
                .fullName(dto.fullName())
                .phone(dto.phone())
                .whatsappPhone(dto.whatsappPhone())
                .registrationAddress(dto.registrationAddress())
                .livingAddress(dto.livingAddress())
                .passportNumber(dto.passportNumber())
                .passportIssuedAt(dto.passportIssuedAt())
                .pin(dto.pin())
                .birthYear(dto.birthYear())
                .email(dto.email())
                .comment(dto.comment())
                .tags(EnumSet.of(ClientTag.CLIENT))
                .build();

        clientRepository.save(client);
        return toDto(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDto> search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return clientRepository.findAll().stream().map(this::toDto).toList();
        }
        return clientRepository.findByFullNameContainingIgnoreCase(normalized)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponseDto getById(Long id) {
        return clientRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
    }

    @Transactional
    public void addTag(Long clientId, String tag) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        ClientTag tagValue = parseTag(tag);
        Set<ClientTag> tags = client.getTags();
        if (tags == null || tags.isEmpty()) {
            client.setTags(EnumSet.of(tagValue));
        } else {
            tags.add(tagValue);
        }
    }

    @Transactional
    public void removeTag(Long clientId, String tag) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));

        ClientTag tagValue = parseTag(tag);
        if (client.getTags() != null) {
            client.getTags().remove(tagValue);
        }
    }

    private ClientTag parseTag(String tag) {
        return ClientTag.valueOf(tag.toUpperCase());
    }

    private ClientResponseDto toDto(Client c) {
        return ClientResponseDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .whatsappPhone(c.getWhatsappPhone())
                .passportNumber(c.getPassportNumber())
                .registrationAddress(c.getRegistrationAddress())
                .livingAddress(c.getLivingAddress())
                .birthYear(c.getBirthYear())
                .tags(c.getTags())
                .lastBranch(c.getLastBranch() != null ? c.getLastBranch().getName() : null)
                .build();
    }
}

