package org.misha.authservice.dto.excel;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для генерации Excel договора.
 * Содержит все данные, необходимые для заполнения шаблона.
 */
public record ExcelContractDto(
        String toolFullName,      // <название> <модель> #<номер> (первый инструмент, для обратной совместимости)
        BigDecimal pricePerDay,
        BigDecimal depositAmount,
        BigDecimal purchasePrice,
        Integer quantity,
        ClientExcelDto client,
        RentalExcelDto rental,
        List<ToolExcelDto> tools  // Первые 5 инструментов для таблицы в "Пр №1"
) {
}

