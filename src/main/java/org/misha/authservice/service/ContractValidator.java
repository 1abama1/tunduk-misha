package org.misha.authservice.service;

import org.misha.authservice.dto.ContractRequest;
import org.misha.authservice.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class ContractValidator {

    public void validate(ContractRequest request) {
        if (request == null) {
            throw new BadRequestException("Запрос пуст");
        }
        if (request.getClientId() == null) {
            throw new BadRequestException("Не передан clientId");
        }
        if (request.getContractNumber() == null || request.getContractNumber().isBlank()) {
            throw new BadRequestException("Не передан номер договора");
        }
        if (request.getPrice() == null) {
            throw new BadRequestException("Не передана стоимость");
        }
    }
}

