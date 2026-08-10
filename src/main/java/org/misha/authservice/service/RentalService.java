package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.RentRequest;
import org.misha.authservice.dto.ReturnRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RentalService {

        private final ToolInstanceRepository ToolInstanceRepository;
        private final ClientRepository clientRepository;
        private final RentalDocumentRepository rentalRepository;
        private final ToolRentalGuard toolRentalGuard;

        @Transactional
        public RentalDocument rentTool(RentRequest req) {
                ToolInstance ToolInstance = ToolInstanceRepository.findById(req.toolId())
                                .orElseThrow(() -> new AppException(
                                                "TOOL_NOT_FOUND",
                                                "ToolInstance not found",
                                                HttpStatus.NOT_FOUND));

                toolRentalGuard.ensureAvailableForRental(ToolInstance);

                Client client = clientRepository.findById(req.clientId())
                                .orElseThrow(() -> new AppException(
                                                "CLIENT_NOT_FOUND",
                                                "Client not found",
                                                HttpStatus.NOT_FOUND));

                Double totalPrice = req.pricePerDay() * req.rentDays();
                String contractNumber = generateContractNumber();

                RentalDocument doc = RentalDocument.builder()
                                .client(client)
                                .contractNumber(contractNumber)
                                .startDateTime(LocalDateTime.now())
                                .dailyPrice(req.pricePerDay())
                                .amount(totalPrice)
                                .build();

                rentalRepository.save(doc);

                ToolInstance.setContract(doc);
                ToolInstanceRepository.save(ToolInstance);

                doc.setToolId(ToolInstance.getId());
                rentalRepository.save(doc);

                return doc;
        }

        @Transactional
        public void returnTool(ReturnRequest req) {
                RentalDocument doc = rentalRepository.findById(req.contractId())
                                .orElseThrow(() -> new AppException(
                                                "CONTRACT_NOT_FOUND",
                                                "Contract not found",
                                                HttpStatus.NOT_FOUND));

                if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
                        throw new AppException(
                                        "CONTRACT_ALREADY_CLOSED",
                                        "Contract is already closed or terminated",
                                        HttpStatus.BAD_REQUEST);
                }

                ToolInstance ToolInstance = ToolInstanceRepository.findByContractId(req.contractId())
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new AppException(
                                                "TOOL_NOT_FOUND",
                                                "ToolInstance not found for this contract",
                                                HttpStatus.NOT_FOUND));

                doc.setToolId(ToolInstance.getId());
                ToolInstance.setContract(null);
                
                ToolInstanceRepository.save(ToolInstance);

                doc.setReturnDate(LocalDateTime.now());
                rentalRepository.save(doc);
        }

        private String generateContractNumber() {
                LocalDate today = LocalDate.now();
                LocalDateTime startOfDay = today.atStartOfDay();
                LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

                long countToday = rentalRepository.countCreatedBetween(startOfDay, endOfDay);
                long next = countToday + 1;

                return "R-" + today + "-" + String.format("%03d", next);
        }
}

