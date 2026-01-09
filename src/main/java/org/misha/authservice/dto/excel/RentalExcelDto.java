package org.misha.authservice.dto.excel;

/**
 * DTO для данных аренды в Excel.
 */
public record RentalExcelDto(
        String startDate,        // Форматированная дата (dd.MM.yyyy)
        String actualReturnDate, // Форматированная дата (dd.MM.yyyy) или null
        String actualReturnTime  // Форматированное время (HH:mm) или null
) {
}

