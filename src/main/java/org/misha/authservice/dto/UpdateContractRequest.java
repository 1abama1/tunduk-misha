package org.misha.authservice.dto;

import java.time.LocalDate;

public record UpdateContractRequest(
        LocalDate expectedReturnDate,
        Double amount,
        String comment
) {
}


