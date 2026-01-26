package org.misha.authservice.dto;

public record CloseContractRequest(
        Double paidAmount,
        String comment) {
}
