package org.misha.authservice.service;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.stats.StatsSummaryDto;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsService {

        private final RentalDocumentRepository contractRepository;
        private final ToolInstanceRepository toolInstanceRepository;

        @Transactional(readOnly = true)
        public StatsSummaryDto getSummary() {
                long active = contractRepository.countActive();
                long available = toolInstanceRepository.countByContractIsNull();
                long rented = toolInstanceRepository.countByContractNotNull();
                long broken = 0; // Not tracked in new UUID schema yet

                var today = LocalDate.now();
                double todayRevenue = contractRepository.sumAmountClosedOnDate(today);
                double monthRevenue = contractRepository.sumAmountClosedInMonth(today.getMonthValue(), today.getYear());

                return new StatsSummaryDto(
                                active,
                                available,
                                rented,
                                broken,
                                todayRevenue,
                                monthRevenue);
        }
}
