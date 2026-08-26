package org.misha.authservice.dto.excel;

import java.math.BigDecimal;

/**
 * DTO одного инструмента для строки в Excel-таблице (лист "Пр №1", строки 21-25).
 */
public record ToolExcelDto(
        String name,           // Наименование (название модели + инвентарный номер)
        String inventoryNumber, // Завод. / индеф. №
        int quantity,           // Кол-Во (всегда 1 для одного экземпляра)
        BigDecimal pricePerDay  // Стоимость оборудования (цена за сутки)
) {
}
