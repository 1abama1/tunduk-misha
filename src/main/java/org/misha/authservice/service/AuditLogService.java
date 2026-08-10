package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * РЎРµСЂРІРёСЃ РґР»СЏ audit logging РєСЂРёС‚РёС‡РµСЃРєРёС… РѕРїРµСЂР°С†РёР№.
 * Р›РѕРіРёСЂСѓРµС‚ СЃРѕР·РґР°РЅРёРµ, РёР·РјРµРЅРµРЅРёРµ Рё СѓРґР°Р»РµРЅРёРµ РґРѕРіРѕРІРѕСЂРѕРІ, РёРЅСЃС‚СЂСѓРјРµРЅС‚РѕРІ, РєР»РёРµРЅС‚РѕРІ Рё РґРѕРєСѓРјРµРЅС‚РѕРІ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    /**
     * Р›РѕРіРёСЂСѓРµС‚ СЃРѕР·РґР°РЅРёРµ СЃСѓС‰РЅРѕСЃС‚Рё.
     *
     * @param entityType С‚РёРї СЃСѓС‰РЅРѕСЃС‚Рё (Contract, ToolInstance, Client, Document)
     * @param entityId ID СЃРѕР·РґР°РЅРЅРѕР№ СЃСѓС‰РЅРѕСЃС‚Рё
     * @param details РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹Рµ РґРµС‚Р°Р»Рё
     */
    public void logCreate(String entityType, Long entityId, Map<String, Object> details) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "CREATE");
        logData.put("entityType", entityType);
        logData.put("entityId", entityId);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        if (details != null) {
            logData.putAll(details);
        }
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ РѕР±РЅРѕРІР»РµРЅРёРµ СЃСѓС‰РЅРѕСЃС‚Рё.
     *
     * @param entityType С‚РёРї СЃСѓС‰РЅРѕСЃС‚Рё
     * @param entityId ID РѕР±РЅРѕРІР»РµРЅРЅРѕР№ СЃСѓС‰РЅРѕСЃС‚Рё
     * @param details РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹Рµ РґРµС‚Р°Р»Рё (РЅР°РїСЂРёРјРµСЂ, РёР·РјРµРЅРµРЅРЅС‹Рµ РїРѕР»СЏ)
     */
    public void logUpdate(String entityType, Long entityId, Map<String, Object> details) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "UPDATE");
        logData.put("entityType", entityType);
        logData.put("entityId", entityId);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        if (details != null) {
            logData.putAll(details);
        }
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ СѓРґР°Р»РµРЅРёРµ СЃСѓС‰РЅРѕСЃС‚Рё.
     *
     * @param entityType С‚РёРї СЃСѓС‰РЅРѕСЃС‚Рё
     * @param entityId ID СѓРґР°Р»РµРЅРЅРѕР№ СЃСѓС‰РЅРѕСЃС‚Рё
     * @param details РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹Рµ РґРµС‚Р°Р»Рё
     */
    public void logDelete(String entityType, Long entityId, Map<String, Object> details) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "DELETE");
        logData.put("entityType", entityType);
        logData.put("entityId", entityId);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        if (details != null) {
            logData.putAll(details);
        }
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ Р·Р°РєСЂС‹С‚РёРµ РґРѕРіРѕРІРѕСЂР°.
     *
     * @param contractId ID РґРѕРіРѕРІРѕСЂР°
     * @param details РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹Рµ РґРµС‚Р°Р»Рё
     */
    public void logContractClose(Long contractId, Map<String, Object> details) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "CLOSE_CONTRACT");
        logData.put("entityType", "Contract");
        logData.put("entityId", contractId);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        if (details != null) {
            logData.putAll(details);
        }
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ СЂР°СЃС‚РѕСЂР¶РµРЅРёРµ РґРѕРіРѕРІРѕСЂР°.
     *
     * @param contractId ID РґРѕРіРѕРІРѕСЂР°
     * @param reason РїСЂРёС‡РёРЅР° СЂР°СЃС‚РѕСЂР¶РµРЅРёСЏ
     * @param details РґРѕРїРѕР»РЅРёС‚РµР»СЊРЅС‹Рµ РґРµС‚Р°Р»Рё
     */
    public void logContractTerminate(Long contractId, String reason, Map<String, Object> details) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "TERMINATE_CONTRACT");
        logData.put("entityType", "Contract");
        logData.put("entityId", contractId);
        logData.put("reason", reason);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        if (details != null) {
            logData.putAll(details);
        }
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ Р·Р°РіСЂСѓР·РєСѓ РґРѕРєСѓРјРµРЅС‚Р°.
     *
     * @param documentType С‚РёРї РґРѕРєСѓРјРµРЅС‚Р°
     * @param entityId ID СЃСѓС‰РЅРѕСЃС‚Рё, Рє РєРѕС‚РѕСЂРѕР№ РїСЂРёРІСЏР·Р°РЅ РґРѕРєСѓРјРµРЅС‚
     * @param fileName РёРјСЏ С„Р°Р№Р»Р°
     */
    public void logDocumentUpload(String documentType, Long entityId, String fileName) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "UPLOAD_DOCUMENT");
        logData.put("documentType", documentType);
        logData.put("entityId", entityId);
        logData.put("fileName", fileName);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        log.info("AUDIT: {}", logData);
    }

    /**
     * Р›РѕРіРёСЂСѓРµС‚ Р·Р°РіСЂСѓР·РєСѓ РёР·РѕР±СЂР°Р¶РµРЅРёСЏ.
     *
     * @param imageType С‚РёРї РёР·РѕР±СЂР°Р¶РµРЅРёСЏ (ToolInstance, Client)
     * @param entityId ID СЃСѓС‰РЅРѕСЃС‚Рё
     * @param fileName РёРјСЏ С„Р°Р№Р»Р°
     */
    public void logImageUpload(String imageType, Long entityId, String fileName) {
        String username = getCurrentUsername();
        Map<String, Object> logData = new HashMap<>();
        logData.put("action", "UPLOAD_IMAGE");
        logData.put("imageType", imageType);
        logData.put("entityId", entityId);
        logData.put("fileName", fileName);
        logData.put("username", username);
        logData.put("timestamp", LocalDateTime.now());
        log.info("AUDIT: {}", logData);
    }

    /**
     * РџРѕР»СѓС‡Р°РµС‚ РёРјСЏ С‚РµРєСѓС‰РµРіРѕ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РёР· SecurityContext.
     *
     * @return РёРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РёР»Рё "SYSTEM" РµСЃР»Рё РЅРµ Р°СѓС‚РµРЅС‚РёС„РёС†РёСЂРѕРІР°РЅ
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    return (String) principal;
                }
                // Р•СЃР»Рё principal - СЌС‚Рѕ UserDetails, РјРѕР¶РЅРѕ РїРѕР»СѓС‡РёС‚СЊ username
                return principal.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to get current username for audit log", e);
        }
        return "SYSTEM";
    }
}


