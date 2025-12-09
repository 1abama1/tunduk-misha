package org.misha.authservice.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.stats.StatsSummaryDto;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final RentalDocumentRepository contractRepository;
    private final ToolRepository toolRepository;

    @Transactional(readOnly = true)
    public StatsSummaryDto getSummary() {
        long active = contractRepository.findAllActive().size();
        long available = toolRepository.findByStatus(ToolStatus.AVAILABLE).size();
        long rented = toolRepository.findByStatus(ToolStatus.RENTED).size();
        long broken = toolRepository.findByStatus(ToolStatus.BROKEN).size();

        var today = LocalDate.now();
        double todayRevenue = contractRepository.findAll().stream()
                .filter(c -> c.getClosedAt() != null && c.getClosedAt().toLocalDate().equals(today))
                .mapToDouble(c -> c.getAmount() == null ? 0 : c.getAmount())
                .sum();

        double monthRevenue = contractRepository.findAll().stream()
                .filter(c -> c.getClosedAt() != null && c.getClosedAt().getMonth().equals(today.getMonth()))
                .mapToDouble(c -> c.getAmount() == null ? 0 : c.getAmount())
                .sum();

        return new StatsSummaryDto(
                active,
                available,
                rented,
                broken,
                todayRevenue,
                monthRevenue
        );
    }
}

