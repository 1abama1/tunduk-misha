package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.dto.CreateDocumentRequest;
import org.misha.authservice.dto.UpdateDocumentRequest;
import org.misha.authservice.entity.RentalDocument;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.ClientRepository;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolCategoryRepository;
import org.misha.authservice.repository.ToolInstanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalDocumentService {
    private final RentalWriteLock rentalWriteLock;

    private final RentalDocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolAvailabilityService availabilityService;
    private final ToolRentalGuard toolRentalGuard;

    // -------- CREATE --------
    @Transactional
    public RentalDocument create(CreateDocumentRequest req) {
        rentalWriteLock.acquire();


        if (documentRepository.existsByContractNumber(req.getContractNumber())) {
            throw new AppException("CONTRACT_EXISTS", "Операция с договором недоступна", HttpStatus.CONFLICT);
        }

        var client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));

        RentalValidation.client(client);
        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(req.getContractNumber())
                .startDateTime(LocalDateTime.now())
                .build();

        documentRepository.save(doc);


        if (req.getToolId() != null) {
            var ToolInstance = ToolInstanceRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND));


            if (ToolInstance.getTemplate() == null) {
                throw new AppException("TOOL_TEMPLATE_MISSING", "ToolInstance template is not defined", HttpStatus.BAD_REQUEST);
            }

            UUID templateId = ToolInstance.getTemplate().getId();
            if (!availabilityService.isAvailable(templateId)) {
                throw new AppException("TOOL_NOT_AVAILABLE", "Операция с договором недоступна",
                        HttpStatus.BAD_REQUEST);
            }


            if (req.getCategoryId() != null) {
                var category = categoryRepository.findById(req.getCategoryId())
                        .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Category not found",
                                HttpStatus.NOT_FOUND));


                if (ToolInstance.getTemplate() == null || ToolInstance.getTemplate().getCategory() == null ||
                        !ToolInstance.getTemplate().getCategory().getId().equals(category.getId())) {
                    throw new AppException("TOOL_CATEGORY_MISMATCH", "ToolInstance does not belong to selected category",
                            HttpStatus.BAD_REQUEST);
                }
            }


            toolRentalGuard.ensureAvailableForRental(ToolInstance);


            ToolInstance.setContract(doc);
            ToolInstanceRepository.save(ToolInstance);


            doc.getHistoricalToolIds().add(ToolInstance.getId());
            doc.setToolId(ToolInstance.getId());
            documentRepository.save(doc);
        }


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
        rentalWriteLock.acquire();

        RentalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null)
            throw new AppException("CONTRACT_CLOSED", "Нельзя редактировать закрытый договор", HttpStatus.CONFLICT);
        if (req.getAmount() != null && (!Double.isFinite(req.getAmount()) || req.getAmount() < 0))
            throw new AppException("INVALID_AMOUNT", "Сумма должна быть конечной и неотрицательной", HttpStatus.BAD_REQUEST);
        if (req.getContractNumber() != null)
            doc.setContractNumber(req.getContractNumber());

        if (req.getStartDateTime() != null)
            doc.setStartDateTime(req.getStartDateTime());

        if (req.getAmount() != null)
            doc.setAmount(req.getAmount());


        if (req.getToolId() != null && !java.util.Objects.equals(req.getToolId(), doc.getToolId()))
            throw new AppException("IMMUTABLE_COMPOSITION", "Состав выданного договора не меняется. Закройте его и создайте новый.", HttpStatus.CONFLICT);
        if (req.getToolId() != null && doc.getToolId() == null) {
            var newToolInstance = ToolInstanceRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND));

            if (!java.util.Objects.equals(req.getToolId(), doc.getToolId())) toolRentalGuard.ensureAvailableForRental(newToolInstance);
            if (newToolInstance.getContract() != null && !newToolInstance.getContract().getId().equals(doc.getId()))
                throw new AppException("TOOL_IN_OTHER_DOCUMENT", "ToolInstance belongs to another document",
                        HttpStatus.CONFLICT);


            if (doc.getTools() != null) {
                doc.getTools().forEach(t -> {
                    t.setContract(null);
                    ToolInstanceRepository.save(t);
                });
            }


            newToolInstance.setContract(doc);
            ToolInstanceRepository.save(newToolInstance);


            doc.getHistoricalToolIds().add(newToolInstance.getId());
            doc.setToolId(newToolInstance.getId());
        }

        documentRepository.save(doc);


        return documentRepository.findByIdWithTools(doc.getId())
                .orElse(doc);
    }


    @Transactional
    public RentalDocument close(Long docId) {
        rentalWriteLock.acquire();
        RentalDocument doc = documentRepository.findByIdWithTools(docId)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Операция с договором недоступна",
                    HttpStatus.BAD_REQUEST);
        }


        if (doc.getTools() != null && !doc.getTools().isEmpty()) {
            ToolInstance firstToolInstance = doc.getTools().get(0);
            doc.setToolId(firstToolInstance.getId());


            doc.getTools().forEach(ToolInstance -> {
                doc.getHistoricalToolIds().add(ToolInstance.getId());
                ToolInstance.setContract(null);
                ToolInstanceRepository.save(ToolInstance);
            });
        }


        doc.setReturnDate(LocalDateTime.now());
        documentRepository.save(doc);


        return documentRepository.findByIdWithTools(docId)
                .orElse(doc);
    }

    // -------- DELETE --------
    @Transactional
    public void delete(Long id) {
        rentalWriteLock.acquire();

        var doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));


        if (doc.getTools() != null) {
            doc.getTools().forEach(t -> t.setContract(null));
            ToolInstanceRepository.saveAll(doc.getTools());
        }

        if (doc.getReturnDate() == null && doc.getTerminatedAt() == null)
            throw new AppException("ACTIVE_DOCUMENT", "Сначала закройте договор", HttpStatus.CONFLICT);
        documentRepository.delete(doc);
    }
}

