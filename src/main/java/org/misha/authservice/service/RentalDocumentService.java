package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateDocumentRequest;
import org.misha.authservice.dto.UpdateDocumentRequest;
import org.misha.authservice.entity.Client;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolCategory;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalDocumentService {

    private final RentalDocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final ToolRepository toolRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolAvailabilityService availabilityService;
    private final ToolRentalGuard toolRentalGuard;

    // -------- CREATE --------
    @Transactional
    public RentalDocument create(CreateDocumentRequest req) {

        // Проверка на существование номера контракта
        if (documentRepository.existsByContractNumber(req.getContractNumber())) {
            throw new AppException("CONTRACT_EXISTS", "Такой номер контракта уже существует", HttpStatus.CONFLICT);
        }

        var client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));

        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(req.getContractNumber())
                .startDateTime(LocalDateTime.now())
                .build();

        documentRepository.save(doc);

        // Если передан toolId, привязываем инструмент к документу
        if (req.getToolId() != null) {
            var tool = toolRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

            // Проверить наличие свободных инструментов
            if (tool.getTemplate() == null) {
                throw new AppException("TOOL_TEMPLATE_MISSING", "Tool template is not defined", HttpStatus.BAD_REQUEST);
            }

            Long templateId = tool.getTemplate().getId();
            if (!availabilityService.isAvailable(templateId)) {
                throw new AppException("TOOL_NOT_AVAILABLE", "Инструменты данного типа закончились", HttpStatus.BAD_REQUEST);
            }

            // Если передан categoryId, проверяем соответствие
            if (req.getCategoryId() != null) {
                var category = categoryRepository.findById(req.getCategoryId())
                        .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Category not found", HttpStatus.NOT_FOUND));

                // Проверяем что инструмент подходит под категорию
                if (tool.getTemplate() == null || tool.getTemplate().getCategory() == null ||
                        !tool.getTemplate().getCategory().getId().equals(category.getId())) {
                    throw new AppException("TOOL_CATEGORY_MISMATCH", "Tool does not belong to selected category", HttpStatus.BAD_REQUEST);
                }
            }

            // Проверяем что инструмент не в аренде
            toolRentalGuard.ensureAvailableForRental(tool);

            // Привязываем инструмент к документу
            tool.setContract(doc);
            toolRepository.save(tool);
        }

        // Перезагружаем документ с инструментами
        return documentRepository.findByIdWithTools(doc.getId())
                .orElse(doc);
    }

    // -------- READ ALL --------
    @Transactional(readOnly = true)
    public List<RentalDocument> findAll() {
        return documentRepository.findAllWithTools();
    }

    // -------- READ ONE --------
    @Transactional(readOnly = true)
    public RentalDocument findOne(Long id) {
        return documentRepository.findByIdWithTools(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));
    }

    // -------- UPDATE --------
    @Transactional
    public RentalDocument update(Long id, UpdateDocumentRequest req) {

        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        if (req.getContractNumber() != null)
            doc.setContractNumber(req.getContractNumber());

        if (req.getStartDateTime() != null)
            doc.setStartDateTime(req.getStartDateTime());

        if (req.getExpectedReturnDate() != null)
            doc.setExpectedReturnDate(req.getExpectedReturnDate());

        if (req.getAmount() != null)
            doc.setAmount(req.getAmount());

        // ---------- смена инструмента ----------
        if (req.getToolId() != null) {
            var newTool = toolRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "Tool not found", HttpStatus.NOT_FOUND));

            if (newTool.getContract() != null && !newTool.getContract().getId().equals(doc.getId()))
                throw new AppException("TOOL_IN_OTHER_DOCUMENT", "Tool belongs to another document", HttpStatus.CONFLICT);

            // убрать старые инструменты
            if (doc.getTools() != null) {
                doc.getTools().forEach(t -> {
                    t.setContract(null);
                    toolRepository.save(t);
                });
            }

            // привязать новый
            newTool.setContract(doc);
            toolRepository.save(newTool);
        }

        documentRepository.save(doc);
        
        // Перезагружаем документ с инструментами
        return documentRepository.findByIdWithTools(doc.getId())
                .orElse(doc);
    }

    // -------- CLOSE (возврат инструмента) --------
    @Transactional
    public RentalDocument close(Long docId) {
        RentalDocument doc = documentRepository.findByIdWithTools(docId)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        if (doc.getClosedAt() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Договор уже завершён",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Отвязать все инструменты от документа
        if (doc.getTools() != null && !doc.getTools().isEmpty()) {
            doc.getTools().forEach(tool -> {
                tool.setContract(null);
                toolRepository.save(tool);
            });
        }

        // Установить дату закрытия договора
        doc.setClosedAt(LocalDateTime.now());
        documentRepository.save(doc);

        // Перезагружаем документ с инструментами
        return documentRepository.findByIdWithTools(docId)
                .orElse(doc);
    }

    // -------- DELETE --------
    @Transactional
    public void delete(Long id) {

        var doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        // Отвязать инструменты
        if (doc.getTools() != null) {
            doc.getTools().forEach(t -> t.setContract(null));
            toolRepository.saveAll(doc.getTools());
        }

        documentRepository.delete(doc);
    }
}
