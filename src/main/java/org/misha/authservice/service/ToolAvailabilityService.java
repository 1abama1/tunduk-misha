package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.exception.AppException;
import org.misha.authservice.repository.RentalDocumentRepository;
import org.misha.authservice.repository.ToolRepository;
import org.misha.authservice.repository.ToolTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для проверки доступности инструментов.
 *
 * <p><b>Алгоритм доступности по датам:</b>
 * <pre>
 * available = total(не списанных) - inRepair(IN_REPAIR + BROKEN) - busy(по датам)
 * </pre>
 * где busy — количество экземпляров, договоры которых пересекаются
 * с запрошенным периодом [startDate, endDate].
 *
 * <p><b>Условие пересечения</b> (классика интервалов):
 * {@code contractStart <= reqEnd AND contractEnd >= reqStart}
 */
@Service
@RequiredArgsConstructor
public class ToolAvailabilityService {

    private final ToolRepository toolRepository;
    private final ToolTemplateRepository templateRepository;
    private final RentalDocumentRepository rentalDocumentRepository;

    // ──────────────────────────────────────────────────────────
    // Текущая доступность (без учёта дат) — для обратной совместимости
    // ──────────────────────────────────────────────────────────

    /**
     * Возвращает количество СЕЙЧАС свободных экземпляров шаблона
     * (contract == null, без учёта будущих броней).
     */
    @Transactional(readOnly = true)
    public int getAvailableCount(Long templateId) {
        validateTemplate(templateId);
        long total = toolRepository.countByTemplateId(templateId);
        long rented = toolRepository.countByTemplateIdAndContractNotNull(templateId);
        return (int) (total - rented);
    }

    /**
     * Возвращает true, если хотя бы один экземпляр шаблона доступен прямо сейчас.
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(Long templateId) {
        return getAvailableCount(templateId) > 0;
    }

    /**
     * Возвращает список СЕЙЧАС свободных экземпляров.
     */
    @Transactional(readOnly = true)
    public List<Tool> getAvailableTools(Long templateId) {
        validateTemplate(templateId);
        return toolRepository.findByTemplateIdAndContractIsNull(templateId);
    }

    // ──────────────────────────────────────────────────────────
    // Date-aware доступность (с учётом пересечения дат)
    // ──────────────────────────────────────────────────────────

    /**
     * Рассчитывает количество свободных экземпляров шаблона на указанный период.
     *
     * <p>Формула:
     * <pre>
     * available = total(не DECOMMISSIONED)
     *           - count(IN_REPAIR + BROKEN)    ← в ремонте на неопределённый срок
     *           - count(занятых по договорам, чьи даты пересекаются)
     * </pre>
     *
     * @param templateId ID шаблона (ToolTemplate)
     * @param startDate  начало желаемого периода аренды
     * @param endDate    конец желаемого периода аренды
     * @return количество доступных экземпляров (не менее 0)
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("deprecation")
    public int getAvailableCount(Long templateId, LocalDateTime startDate, LocalDateTime endDate) {
        validateTemplate(templateId);

        // 1. Всего "живых" экземпляров (не списанных)
        long total = toolRepository.countByTemplateId(templateId)
                - toolRepository.countByTemplateIdAndStatus(templateId, ToolStatus.DECOMMISSIONED);

        // 2. Экземпляры в ремонте (недоступны на любой период пока статус не поменяют)
        long inRepair = toolRepository.countByTemplateIdAndStatus(templateId, ToolStatus.IN_REPAIR)
                + toolRepository.countByTemplateIdAndStatus(templateId, ToolStatus.BROKEN);

        // 3. Экземпляры с активными договорами, пересекающимися с нужными датами
        int busyInRange = rentalDocumentRepository.countBusyToolsByTemplateAndDates(
                templateId, startDate, endDate);

        int available = (int) (total - inRepair - busyInRange);
        return Math.max(available, 0);
    }

    /**
     * Возвращает true, если хотя бы один экземпляр шаблона доступен на указанный период.
     *
     * @param templateId ID шаблона
     * @param startDate  начало желаемого периода
     * @param endDate    конец желаемого периода
     * @return true, если есть хотя бы один свободный экземпляр
     */
    @Transactional(readOnly = true)
    public boolean isAvailableForPeriod(Long templateId, LocalDateTime startDate, LocalDateTime endDate) {
        return getAvailableCount(templateId, startDate, endDate) > 0;
    }

    /**
     * Находит все свободные экземпляры заданного шаблона на указанный период.
     * Удобен для отображения списка доступных инструментов клиенту.
     *
     * @param templateId ID шаблона
     * @param startDate  начало периода
     * @param endDate    конец периода
     * @return список свободных экземпляров, отсортированных по инвентарному номеру
     */
    @Transactional(readOnly = true)
    public List<Tool> getAvailableToolsForPeriod(Long templateId, LocalDateTime startDate, LocalDateTime endDate) {
        validateTemplate(templateId);
        return toolRepository.findAvailableForPeriod(templateId, startDate, endDate);
    }

    // ──────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────

    private void validateTemplate(Long templateId) {
        templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException(
                        "TEMPLATE_NOT_FOUND",
                        "Template not found: " + templateId,
                        HttpStatus.NOT_FOUND));
    }
}
