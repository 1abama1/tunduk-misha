package org.misha.authservice.dto.excel;

import java.math.BigDecimal;

/**
 * DTO для генерации Excel договора.
 * Содержит все данные, необходимые для заполнения шаблона.
 */
public record ExcelContractDto(
        String toolFullName,      // <название> <модель> #<номер>
        BigDecimal pricePerDay,
        BigDecimal depositAmount,
        ClientExcelDto client,
        RentalExcelDto rental
) {
}

