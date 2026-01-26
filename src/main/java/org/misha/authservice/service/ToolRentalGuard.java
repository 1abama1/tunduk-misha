package org.misha.authservice.service;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.Tool;
import org.misha.authservice.entity.ToolStatus;
import org.misha.authservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Единая точка проверки статуса аренды инструментов.
 * Устраняет дублирование проверок tool.getContract() != null по всему коду.
 */
@Component
@RequiredArgsConstructor
public class ToolRentalGuard {

    /**
     * Проверяет, что инструмент не находится в аренде.
     * Выбрасывает исключение, если инструмент арендован.
     *
     * @param tool инструмент для проверки
     * @throws AppException если инструмент в аренде
     */
    public void ensureNotRented(Tool tool) {
        if (isRented(tool)) {
            throw new AppException(
                    "TOOL_IS_RENTED",
                    "Инструмент находится в аренде",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Проверяет, что инструмент не находится в аренде.
     * Выбрасывает исключение с кастомным сообщением.
     *
     * @param tool    инструмент для проверки
     * @param message кастомное сообщение об ошибке
     * @throws AppException если инструмент в аренде
     */
    public void ensureNotRented(Tool tool, String message) {
        if (isRented(tool)) {
            throw new AppException(
                    "TOOL_IS_RENTED",
                    message,
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Проверяет, что инструмент доступен для аренды.
     * Выбрасывает исключение, если инструмент уже арендован.
     *
     * @param tool инструмент для проверки
     * @throws AppException если инструмент уже арендован
     */
    public void ensureAvailableForRental(Tool tool) {
        if (isRented(tool)) {
            throw new AppException(
                    "TOOL_ALREADY_RENTED",
                    "Инструмент уже арендован",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Проверяет, можно ли удалить инструмент.
     * Инструмент нельзя удалить, если он в аренде.
     *
     * @param tool инструмент для проверки
     * @throws AppException если инструмент в аренде
     */
    public void ensureCanDelete(Tool tool) {
        if (isRented(tool)) {
            throw new AppException(
                    "CANNOT_DELETE_RENTED_TOOL",
                    "Нельзя удалить инструмент, который находится в аренде",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Проверяет, можно ли изменить статус инструмента.
     * Нельзя изменить статус, если инструмент в аренде.
     *
     * @param tool инструмент для проверки
     * @throws AppException если инструмент в аренде
     */
    public void ensureCanChangeStatus(Tool tool) {
        if (isRented(tool)) {
            throw new AppException(
                    "TOOL_IS_RENTED",
                    "Нельзя изменить статус инструмента в аренде",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Проверяет, можно ли редактировать инструмент.
     * Нельзя редактировать инструмент, если он в аренде или просрочен.
     *
     * @param tool инструмент для проверки
     * @throws AppException если инструмент в аренде или просрочен
     */
    public void ensureCanEdit(Tool tool) {
        ToolStatus status = resolveStatus(tool);
        if (status == ToolStatus.RENTED || status == ToolStatus.OVERDUE) {
            throw new AppException(
                    "TOOL_IN_USE",
                    "Нельзя редактировать инструмент в аренде",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private final org.misha.authservice.repository.RentalDocumentRepository documentRepository;

    /**
     * Проверяет, находится ли инструмент в аренде.
     *
     * @param tool инструмент для проверки
     * @return true, если инструмент в аренде (есть активный договор)
     */
    public boolean isRented(Tool tool) {
        // 1. Проверка через прямую связь в объекте Tool (самый быстрый способ)
        if (tool.getContract() != null) {
            // Договор считается активным, если он не закрыт и не расторгнут
            if (tool.getContract().getReturnDate() == null
                    && tool.getContract().getTerminatedAt() == null) {
                return true;
            }
        }

        // 2. Проверка по таблице документов (на случай, если связь в таблице tools была
        // затерта
        // или инструмент еще не привязан, но договор уже создан в этой транзакции)
        return documentRepository.existsByToolIdAndReturnDateIsNullAndTerminatedAtIsNull(tool.getId());
    }

    /**
     * Определяет текущий статус инструмента на основе договора.
     *
     * @param tool инструмент
     * @return статус инструмента
     */
    public ToolStatus resolveStatus(Tool tool) {
        if (tool.getContract() == null) {
            return ToolStatus.AVAILABLE;
        }

        if (tool.getContract().getTerminatedAt() != null) {
            return ToolStatus.AVAILABLE;
        }

        return ToolStatus.RENTED;
    }
}
