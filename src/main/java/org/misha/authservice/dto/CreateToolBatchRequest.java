package org.misha.authservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на пакетное создание экземпляров инструментов.
 * Сервис автоматически вычислит последний инвентарный номер для данного шаблона
 * и создаст {@code count} новых экземпляров с инкрементом номера.
 */
public record CreateToolBatchRequest(
        @NotNull(message = "templateId is required")
        Long templateId,

        @NotNull(message = "count is required")
        @Min(value = 1, message = "count must be at least 1")
        Integer count,

        /** Опциональная базовая цена за сутки для всех создаваемых экземпляров */
        Double dailyPrice,

        /** Опциональный залог для всех создаваемых экземпляров */
        Double deposit,

        /** Опциональная закупочная цена для всех создаваемых экземпляров */
        Double purchasePrice
) {}
