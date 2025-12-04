package org.misha.authservice.mapper;

import org.misha.authservice.dto.ClientDto;
import org.misha.authservice.dto.ClientImageDto;
import org.misha.authservice.dto.DocumentDto;
import org.misha.authservice.dto.PassportDto;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ClientImage;
import org.misha.authservice.entity.ClientPassport;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ClientMapper {

    public ClientDto toDto(Client c) {
        return ClientDto.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .address(c.getAddress())
                .email(c.getEmail())
                .birthDate(c.getBirthDate())
                .comment(c.getComment())
                .passport(toPassportDto(c.getPassport()))
                .tag(c.getTag() != null ? c.getTag().name() : null)
                .documents(
                        c.getDocuments() == null || c.getDocuments().isEmpty() 
                                ? new ArrayList<>() 
                                : c.getDocuments().stream()
                                        .map(this::toDocDto)
                                        .toList()
                )
                .build();
    }

    public DocumentDto toDocDto(RentalDocument d) {
        Tool tool = d.getTools() != null && !d.getTools().isEmpty() 
                ? d.getTools().get(0) 
                : null;

        String categoryName = null;
        String toolName = null;
        String serialNumber = null;

        if (tool != null && tool.getTemplate() != null) {
            toolName = tool.getTemplate().getName();
            serialNumber = tool.getSerialNumber();
            if (tool.getTemplate().getCategory() != null) {
                categoryName = tool.getTemplate().getCategory().getName();
            }
        }

        return DocumentDto.builder()
                .id(d.getId())
                .contractNumber(d.getContractNumber())
                .category(categoryName)
                .toolName(toolName)
                .serialNumber(serialNumber)
                .startDateTime(d.getStartDateTime())
                .expectedReturnDate(d.getExpectedReturnDate())
                .amount(d.getAmount())
                .toolId(tool != null ? tool.getId() : null)
                .closedAt(d.getClosedAt())
                .terminatedAt(d.getTerminatedAt())
                .terminationReason(d.getTerminationReason())
                .status(d.getStatus())
                .build();
    }

    private PassportDto toPassportDto(ClientPassport passport) {
        if (passport == null) {
            return null;
        }
        return new PassportDto(
                passport.getSeries(),
                passport.getNumber(),
                passport.getIssuedBy(),
                passport.getSubdivisionCode(),
                passport.getIssueDate(),
                passport.getInn()
        );
    }

    public List<ClientImageDto> toImageDtos(List<ClientImage> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(img -> new ClientImageDto(
                        img.getId(),
                        img.getFileName(),
                        img.getFileType()
                ))
                .toList();
    }
}

