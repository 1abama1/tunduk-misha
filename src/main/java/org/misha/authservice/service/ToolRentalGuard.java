package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.ToolInstance;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToolRentalGuard {

    private final org.misha.authservice.repository.RentalDocumentRepository documentRepository;

    public void ensureNotRented(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            throw new AppException(
                    "TOOL_IS_RENTED",
                    "РРЅСЃС‚СЂСѓРјРµРЅС‚ РЅР°С…РѕРґРёС‚СЃСЏ РІ Р°СЂРµРЅРґРµ",
                    HttpStatus.BAD_REQUEST);
        }
    }

    public void ensureNotRented(ToolInstance ToolInstance, String message) {
        if (isRented(ToolInstance)) {
            throw new AppException("TOOL_IS_RENTED", message, HttpStatus.BAD_REQUEST);
        }
    }

    public void ensureAvailableForRental(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            throw new AppException("TOOL_ALREADY_RENTED", "РРЅСЃС‚СЂСѓРјРµРЅС‚ СѓР¶Рµ Р°СЂРµРЅРґРѕРІР°РЅ", HttpStatus.CONFLICT);
        }
    }

    public void ensureCanDelete(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            throw new AppException("CANNOT_DELETE_RENTED_TOOL", "РќРµР»СЊР·СЏ СѓРґР°Р»РёС‚СЊ РёРЅСЃС‚СЂСѓРјРµРЅС‚, РєРѕС‚РѕСЂС‹Р№ РЅР°С…РѕРґРёС‚СЃСЏ РІ Р°СЂРµРЅРґРµ", HttpStatus.BAD_REQUEST);
        }
    }

    public boolean isRented(ToolInstance ToolInstance) {
        if (ToolInstance.getContract() != null) {
            if (ToolInstance.getContract().getReturnDate() == null
                    && ToolInstance.getContract().getTerminatedAt() == null) {
                return true;
            }
        }
        return documentRepository.existsByToolIdAndReturnDateIsNullAndTerminatedAtIsNull(ToolInstance.getId());
    }
    
    public String resolveStatus(ToolInstance ToolInstance) {
        if (isRented(ToolInstance)) {
            return "RENTED";
        }
        return ToolInstance.getStatus() != null ? ToolInstance.getStatus().name() : "AVAILABLE";
    }
}

