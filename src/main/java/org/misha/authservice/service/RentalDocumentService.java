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

    private final RentalDocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final ToolInstanceRepository ToolInstanceRepository;
    private final ToolCategoryRepository categoryRepository;
    private final ToolAvailabilityService availabilityService;
    private final ToolRentalGuard toolRentalGuard;

    // -------- CREATE --------
    @Transactional
    public RentalDocument create(CreateDocumentRequest req) {

        // РџСЂРѕРІРµСЂРєР° РЅР° СЃСѓС‰РµСЃС‚РІРѕРІР°РЅРёРµ РЅРѕРјРµСЂР° РєРѕРЅС‚СЂР°РєС‚Р°
        if (documentRepository.existsByContractNumber(req.getContractNumber())) {
            throw new AppException("CONTRACT_EXISTS", "РўР°РєРѕР№ РЅРѕРјРµСЂ РєРѕРЅС‚СЂР°РєС‚Р° СѓР¶Рµ СЃСѓС‰РµСЃС‚РІСѓРµС‚", HttpStatus.CONFLICT);
        }

        var client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new AppException("CLIENT_NOT_FOUND", "Client not found", HttpStatus.NOT_FOUND));

        RentalDocument doc = RentalDocument.builder()
                .client(client)
                .contractNumber(req.getContractNumber())
                .startDateTime(LocalDateTime.now())
                .build();

        documentRepository.save(doc);

        // Р•СЃР»Рё РїРµСЂРµРґР°РЅ toolId, РїСЂРёРІСЏР·С‹РІР°РµРј РёРЅСЃС‚СЂСѓРјРµРЅС‚ Рє РґРѕРєСѓРјРµРЅС‚Сѓ
        if (req.getToolId() != null) {
            var ToolInstance = ToolInstanceRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND));

            // РџСЂРѕРІРµСЂРёС‚СЊ РЅР°Р»РёС‡РёРµ СЃРІРѕР±РѕРґРЅС‹С… РёРЅСЃС‚СЂСѓРјРµРЅС‚РѕРІ
            if (ToolInstance.getTemplate() == null) {
                throw new AppException("TOOL_TEMPLATE_MISSING", "ToolInstance template is not defined", HttpStatus.BAD_REQUEST);
            }

            UUID templateId = ToolInstance.getTemplate().getId();
            if (!availabilityService.isAvailable(templateId)) {
                throw new AppException("TOOL_NOT_AVAILABLE", "РРЅСЃС‚СЂСѓРјРµРЅС‚С‹ РґР°РЅРЅРѕРіРѕ С‚РёРїР° Р·Р°РєРѕРЅС‡РёР»РёСЃСЊ",
                        HttpStatus.BAD_REQUEST);
            }

            // Р•СЃР»Рё РїРµСЂРµРґР°РЅ categoryId, РїСЂРѕРІРµСЂСЏРµРј СЃРѕРѕС‚РІРµС‚СЃС‚РІРёРµ
            if (req.getCategoryId() != null) {
                var category = categoryRepository.findById(req.getCategoryId())
                        .orElseThrow(() -> new AppException("CATEGORY_NOT_FOUND", "Category not found",
                                HttpStatus.NOT_FOUND));

                // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ РёРЅСЃС‚СЂСѓРјРµРЅС‚ РїРѕРґС…РѕРґРёС‚ РїРѕРґ РєР°С‚РµРіРѕСЂРёСЋ
                if (ToolInstance.getTemplate() == null || ToolInstance.getTemplate().getCategory() == null ||
                        !ToolInstance.getTemplate().getCategory().getId().equals(category.getId())) {
                    throw new AppException("TOOL_CATEGORY_MISMATCH", "ToolInstance does not belong to selected category",
                            HttpStatus.BAD_REQUEST);
                }
            }

            // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ РёРЅСЃС‚СЂСѓРјРµРЅС‚ РЅРµ РІ Р°СЂРµРЅРґРµ
            toolRentalGuard.ensureAvailableForRental(ToolInstance);

            // РџСЂРёРІСЏР·С‹РІР°РµРј РёРЅСЃС‚СЂСѓРјРµРЅС‚ Рє РґРѕРєСѓРјРµРЅС‚Сѓ
            ToolInstance.setContract(doc);
            ToolInstanceRepository.save(ToolInstance);

            // РЎРѕС…СЂР°РЅСЏРµРј toolId РІ РґРѕРєСѓРјРµРЅС‚Рµ
            doc.setToolId(ToolInstance.getId());
            documentRepository.save(doc);
        }

        // РџРµСЂРµР·Р°РіСЂСѓР¶Р°РµРј РґРѕРєСѓРјРµРЅС‚ СЃ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°РјРё
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

        if (req.getAmount() != null)
            doc.setAmount(req.getAmount());

        // ---------- СЃРјРµРЅР° РёРЅСЃС‚СЂСѓРјРµРЅС‚Р° ----------
        if (req.getToolId() != null) {
            var newToolInstance = ToolInstanceRepository.findById(req.getToolId())
                    .orElseThrow(() -> new AppException("TOOL_NOT_FOUND", "ToolInstance not found", HttpStatus.NOT_FOUND));

            if (newToolInstance.getContract() != null && !newToolInstance.getContract().getId().equals(doc.getId()))
                throw new AppException("TOOL_IN_OTHER_DOCUMENT", "ToolInstance belongs to another document",
                        HttpStatus.CONFLICT);

            // СѓР±СЂР°С‚СЊ СЃС‚Р°СЂС‹Рµ РёРЅСЃС‚СЂСѓРјРµРЅС‚С‹
            if (doc.getTools() != null) {
                doc.getTools().forEach(t -> {
                    t.setContract(null);
                    ToolInstanceRepository.save(t);
                });
            }

            // РїСЂРёРІСЏР·Р°С‚СЊ РЅРѕРІС‹Р№
            newToolInstance.setContract(doc);
            ToolInstanceRepository.save(newToolInstance);

            // РЎРѕС…СЂР°РЅСЏРµРј toolId РІ РґРѕРєСѓРјРµРЅС‚Рµ
            doc.setToolId(newToolInstance.getId());
        }

        documentRepository.save(doc);

        // РџРµСЂРµР·Р°РіСЂСѓР¶Р°РµРј РґРѕРєСѓРјРµРЅС‚ СЃ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°РјРё
        return documentRepository.findByIdWithTools(doc.getId())
                .orElse(doc);
    }

    // -------- CLOSE (РІРѕР·РІСЂР°С‚ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°) --------
    @Transactional
    public RentalDocument close(Long docId) {
        RentalDocument doc = documentRepository.findByIdWithTools(docId)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        if (doc.getReturnDate() != null || doc.getTerminatedAt() != null) {
            throw new AppException(
                    "CONTRACT_ALREADY_CLOSED",
                    "Р”РѕРіРѕРІРѕСЂ СѓР¶Рµ Р·Р°РІРµСЂС€С‘РЅ",
                    HttpStatus.BAD_REQUEST);
        }

        // РЎРѕС…СЂР°РЅСЏРµРј toolId РїРµСЂРµРґ РѕС‚РІСЏР·РєРѕР№ РёРЅСЃС‚СЂСѓРјРµРЅС‚РѕРІ
        if (doc.getTools() != null && !doc.getTools().isEmpty()) {
            ToolInstance firstToolInstance = doc.getTools().get(0);
            doc.setToolId(firstToolInstance.getId());

            // РћС‚РІСЏР·Р°С‚СЊ РІСЃРµ РёРЅСЃС‚СЂСѓРјРµРЅС‚С‹ РѕС‚ РґРѕРєСѓРјРµРЅС‚Р°
            doc.getTools().forEach(ToolInstance -> {
                ToolInstance.setContract(null);
                ToolInstanceRepository.save(ToolInstance);
            });
        }

        // РЈСЃС‚Р°РЅРѕРІРёС‚СЊ РґР°С‚Сѓ Р·Р°РєСЂС‹С‚РёСЏ РґРѕРіРѕРІРѕСЂР°
        doc.setReturnDate(LocalDateTime.now());
        documentRepository.save(doc);

        // РџРµСЂРµР·Р°РіСЂСѓР¶Р°РµРј РґРѕРєСѓРјРµРЅС‚ СЃ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°РјРё
        return documentRepository.findByIdWithTools(docId)
                .orElse(doc);
    }

    // -------- DELETE --------
    @Transactional
    public void delete(Long id) {

        var doc = documentRepository.findById(id)
                .orElseThrow(() -> new AppException("DOCUMENT_NOT_FOUND", "Document not found", HttpStatus.NOT_FOUND));

        // РћС‚РІСЏР·Р°С‚СЊ РёРЅСЃС‚СЂСѓРјРµРЅС‚С‹
        if (doc.getTools() != null) {
            doc.getTools().forEach(t -> t.setContract(null));
            ToolInstanceRepository.saveAll(doc.getTools());
        }

        documentRepository.delete(doc);
    }
}

