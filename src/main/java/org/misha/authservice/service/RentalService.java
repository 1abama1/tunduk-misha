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
    private final RentalWriteLock rentalWriteLock;

        private final ToolInstanceRepository ToolInstanceRepository;
        private final ClientRepository clientRepository;
        private final RentalDocumentRepository rentalRepository;
        private final ToolRentalGuard toolRentalGuard;

        @Transactional
        public RentalDocument rentTool(RentRequest req) {
        rentalWriteLock.acquire();
                ToolInstance toolInstance = ToolInstanceRepository.findById(req.toolId())
                                .orElseThrow(() -> new AppException(
                                                "TOOL_NOT_FOUND",
                                                "ToolInstance not found",
                                                HttpStatus.NOT_FOUND));

                toolRentalGuard.ensureAvailableForRental(toolInstance);

                Client client = clientRepository.findById(req.clientId())
                                .orElseThrow(() -> new AppException(
                                                "CLIENT_NOT_FOUND",
                                                "Client not found",
                                                HttpStatus.NOT_FOUND));

                RentalValidation.client(client);
                if (req.rentDays() == null || req.rentDays() <= 0 || req.pricePerDay() == null ||
                    !Double.isFinite(req.pricePerDay()) || req.pricePerDay() <= 0 || !Double.isFinite(req.pricePerDay() * req.rentDays()))
                    throw new AppException("INVALID_RENT", "Укажите положительную конечную цену и срок аренды", HttpStatus.BAD_REQUEST);
                Double totalPrice = req.pricePerDay() * req.rentDays();
                String contractNumber = generateContractNumber();

                RentalDocument doc = RentalDocument.builder()
                                .client(client)
                                .contractNumber(contractNumber)
                                .startDateTime(LocalDateTime.now())
                                .dailyPrice(req.pricePerDay())
                                .amount(totalPrice)
                                .toolId(toolInstance.getId())
                                .build();

                doc.getHistoricalToolIds().add(toolInstance.getId());
                rentalRepository.save(doc);

                toolInstance.setContract(doc);
                ToolInstanceRepository.save(toolInstance);

                return doc;
        }

        @Transactional
        public void returnTool(ReturnRequest req) {
        rentalWriteLock.acquire();
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

                ToolInstance toolInstance = ToolInstanceRepository.findByContractId(req.contractId())
                                .stream()
                                .findFirst()
                                .orElseThrow(() -> new AppException(
                                                "TOOL_NOT_FOUND",
                                                "ToolInstance not found for this contract",
                                                HttpStatus.NOT_FOUND));

                doc.setToolId(toolInstance.getId());
                var allTools = ToolInstanceRepository.findByContractId(req.contractId());
                allTools.forEach(t -> { doc.getHistoricalToolIds().add(t.getId()); t.setContract(null); });
                ToolInstanceRepository.saveAll(allTools);
                toolInstance.setContract(null);
                
                ToolInstanceRepository.save(toolInstance);

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

