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
 * Сервис для audit logging критических операций.
 * Логирует создание, изменение и удаление договоров, инструментов, клиентов и документов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    /**
     * Логирует создание сущности.
     *
     * @param entityType тип сущности (Contract, Tool, Client, Document)
     * @param entityId ID созданной сущности
     * @param details дополнительные детали
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
     * Логирует обновление сущности.
     *
     * @param entityType тип сущности
     * @param entityId ID обновленной сущности
     * @param details дополнительные детали (например, измененные поля)
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
     * Логирует удаление сущности.
     *
     * @param entityType тип сущности
     * @param entityId ID удаленной сущности
     * @param details дополнительные детали
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
     * Логирует закрытие договора.
     *
     * @param contractId ID договора
     * @param details дополнительные детали
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
     * Логирует расторжение договора.
     *
     * @param contractId ID договора
     * @param reason причина расторжения
     * @param details дополнительные детали
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
     * Логирует загрузку документа.
     *
     * @param documentType тип документа
     * @param entityId ID сущности, к которой привязан документ
     * @param fileName имя файла
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
     * Логирует загрузку изображения.
     *
     * @param imageType тип изображения (Tool, Client)
     * @param entityId ID сущности
     * @param fileName имя файла
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
     * Получает имя текущего пользователя из SecurityContext.
     *
     * @return имя пользователя или "SYSTEM" если не аутентифицирован
     */
    private String getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof String) {
                    return (String) principal;
                }
                // Если principal - это UserDetails, можно получить username
                return principal.toString();
            }
        } catch (Exception e) {
            log.debug("Failed to get current username for audit log", e);
        }
        return "SYSTEM";
    }
}

