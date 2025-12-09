package org.misha.authservice.dto.stats;

public record StatsSummaryDto(
        long activeContracts,
        long availableTools,
        long rentedTools,
        long brokenTools,
        double revenueToday,
        double revenueMonth
) {
}

