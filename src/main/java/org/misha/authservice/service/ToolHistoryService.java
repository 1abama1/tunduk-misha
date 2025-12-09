package org.misha.authservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.tool.ToolHistoryDto;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToolHistoryService {

    private final RentalDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<ToolHistoryDto> getHistory(Long toolId) {
        return documentRepository.findByToolIdOrderByStartDateTimeDesc(toolId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ToolHistoryDto toDto(RentalDocument doc) {
        return ToolHistoryDto.builder()
                .contractId(doc.getId())
                .clientName(doc.getClient() != null ? doc.getClient().getFullName() : null)
                .startDate(doc.getStartDateTime())
                .expectedReturn(doc.getExpectedReturnDate())
                .closedAt(doc.getClosedAt())
                .amount(doc.getAmount())
                .status(doc.getStatus() != null ? doc.getStatus().name() : null)
                .build();
    }
}

