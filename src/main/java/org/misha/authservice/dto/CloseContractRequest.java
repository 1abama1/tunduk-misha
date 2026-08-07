package org.misha.authservice.dto;

import java.time.LocalDateTime;

public record CloseContractRequest(
        Double paidAmount,
        String comment,
        /** Если true — после возврата инструмент переводится в статус IN_REPAIR */
        boolean isBroken,
        /**
         * Фактическая дата возврата. Если null — используется LocalDateTime.now()
         * (менеджер принял "прямо сейчас").
         */
        LocalDateTime actualReturnDate) {
}
