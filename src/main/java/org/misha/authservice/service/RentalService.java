package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.RentRequest;
import org.misha.authservice.dto.ReturnRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.ContractStatus;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сервис для управления арендой инструментов.
 * Реализует правильную архитектуру: аренда происходит на уровне конкретного экземпляра (Tool),
 * а не модели (ToolTemplate).
 */
@Service
@RequiredArgsConstructor
public class RentalService {

    private final ToolRepository toolRepository;
    private final ClientRepository clientRepository;
    private final RentalDocumentRepository rentalRepository;

    /**
     * Аренда инструмента.
     * Создает договор аренды и привязывает конкретный экземпляр инструмента к договору.
     *
     * @param req запрос на аренду
     * @return созданный договор аренды
     * @throws AppException если инструмент не найден, уже арендован или недоступен
     */
    @Transactional
    public RentalDocument rentTool(RentRequest req) {
        // 1. Проверяем существование инструмента
        Tool tool = toolRepository.findById(req.toolId())
                .orElseThrow(() -> new AppException(
                        "TOOL_NOT_FOUND",
                        "Tool not found",
                        HttpStatus.NOT_FOUND
                ));

        // 2. Проверяем, что инструмент не арендован (contract == null)
        if (tool.getContract() != null) {
            throw new AppException(
                    "TOOL_ALREADY_RENTED",
                    "Tool already rented",
                    HttpStatus.CONFLICT
            );
        }

        // 3. Проверяем, что инструмент доступен для аренды (status == AVAILABLE)
        if (tool.getStatus() != ToolStatus.AVAILABLE) {
            throw new AppException(
                    "TOOL_NOT_AVAILABLE",
                    "Tool is not available for rental. Current status: " + tool.getStatus(),
                    HttpStatus.BAD_REQUEST
            );
        }

        // 4. Проверяем существование клиента
        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new AppException(
                        "CLIENT_NOT_FOUND",
                        "Client not found",
                        HttpStatus.NOT_FOUND
                ));

        // 5. Вычисляем даты и сумму
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(req.rentDays());
        Double totalPrice = req.pricePerDay() * req.rentDays();

        // 6. Генерируем номер договора
        String contractNumber = generateContractNumber();

        // 7. Создаем договор аренды
        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(contractNumber)
                .startDateTime(LocalDateTime.now())
                .expectedReturnDate(endDate)
                .dailyPrice(req.pricePerDay())
                .amount(totalPrice)
                .build();

        rentalRepository.save(doc);

        // 8. Привязываем инструмент к договору и меняем статус
        tool.setContract(doc);
        tool.setStatus(ToolStatus.RENTED);
        toolRepository.save(tool);
        
        // 9. Сохраняем toolId в документе
        doc.setToolId(tool.getId());
        rentalRepository.save(doc);

        return doc;
    }

    /**
     * Возврат инструмента.
     * Освобождает инструмент от договора и закрывает договор.
     *
     * @param req запрос на возврат
     * @throws AppException если договор или инструмент не найдены
     */
    @Transactional
    public void returnTool(ReturnRequest req) {
        // 1. Находим договор
        RentalDocument doc = rentalRepository.findById(req.contractId())
                .orElseThrow(() -> new AppException(
                        "CONTRACT_NOT_FOUND",
                        "Contract not found",
                        HttpStatus.NOT_FOUND
                ));

        // 2. Проверяем, что договор еще активен
        if (doc.getClosedAt() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Contract is already closed or terminated",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3. Находим инструмент, привязанный к этому договору
        Tool tool = toolRepository.findByContractId(req.contractId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(
                        "TOOL_NOT_FOUND",
                        "Tool not found for this contract",
                        HttpStatus.NOT_FOUND
                ));

        // 4. Сохраняем toolId перед отвязкой
        doc.setToolId(tool.getId());
        
        // 5. Освобождаем инструмент
        tool.setContract(null);
        tool.setStatus(ToolStatus.AVAILABLE);
        toolRepository.save(tool);

        // 6. Закрываем договор
        doc.setClosedAt(LocalDateTime.now());
        rentalRepository.save(doc);
    }

    /**
     * Генерирует уникальный номер договора в формате R-YYYY-MM-DD-XXX
     */
    private String generateContractNumber() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        long countToday = rentalRepository.countCreatedBetween(startOfDay, endOfDay);
        long next = countToday + 1;

        return "R-" + today + "-" + String.format("%03d", next);
    }
}

