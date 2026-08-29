package org.misha.authservice.service;

import org.misha.authservice.exception.InvalidPhoneException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Вспомогательный сервис: нормализация телефонных номеров КР и валидация ПИН/ИНН.
 */
@Service
public class HelperService {

    private static final String KG_COUNTRY_CODE = "996";
    private static final int FULL_LENGTH    = 12;  // 996 + 9 цифр
    private static final int LOCAL_LENGTH   = 10;  // 0 + 9 цифр (0700123456)
    private static final int SHORT_LENGTH   = 9;   // 9 цифр без кода (700123456)
    private static final int INTL_LENGTH    = 12;  // уже с кодом 996

    /**
     * Нормализует сырой номер телефона к формату {@code 996XXXXXXXXX}
     * и возвращает готовую ссылку на WhatsApp.
     *
     * <p>Поддерживаемые входные форматы:
     * <ul>
     *   <li>{@code 0700 123 456} → {@code 996700123456}</li>
     *   <li>{@code 700123456}    → {@code 996700123456}</li>
     *   <li>{@code 996700123456} → {@code 996700123456} (без изменений)</li>
     * </ul>
     *
     * @param phoneRaw сырой номер (может содержать пробелы, тире, скобки)
     * @return ссылка вида {@code https://wa.me/996XXXXXXXXX}
     * @throws InvalidPhoneException если номер не соответствует ни одному поддерживаемому формату
     */
    public String generateWhatsAppUrl(String phoneRaw) {
        return "https://wa.me/" + normalizePhone(phoneRaw);
    }

    /**
     * Нормализует сырой номер телефона к формату {@code 996XXXXXXXXX}.
     *
     * <p>Поддерживаемые входные форматы:
     * <ul>
     *   <li>{@code 0700123456}  (10 цифр, ведущий 0)  → {@code 996700123456}</li>
     *   <li>{@code 700123456}   (9 цифр)               → {@code 996700123456}</li>
     *   <li>{@code 996700123456}(12 цифр, с кодом)     → {@code 996700123456}</li>
     * </ul>
     * Пробелы, тире и скобки игнорируются.
     *
     * @param phoneRaw сырой номер телефона
     * @return нормализованный номер {@code 996XXXXXXXXX}
     * @throws InvalidPhoneException если номер не соответствует ни одному формату КР
     */
    public String normalizePhone(String phoneRaw) {
        if (!StringUtils.hasText(phoneRaw)) {
            throw new InvalidPhoneException(phoneRaw);
        }

        // Убираем все нецифровые символы (пробелы, тире, скобки, +)
        String clean = phoneRaw.replaceAll("\\D", "");

        // 0700123456  (10 цифр, ведущий 0) → 996700123456
        if (clean.length() == LOCAL_LENGTH && clean.startsWith("0")) {
            clean = KG_COUNTRY_CODE + clean.substring(1);
        }
        // 700123456   (9 цифр без кода) → 996700123456
        else if (clean.length() == SHORT_LENGTH) {
            clean = KG_COUNTRY_CODE + clean;
        }
        // РФ: 89501234567 (11 цифр, ведущая 8) → 79501234567
        else if (clean.length() == 11 && clean.startsWith("8")) {
            clean = "7" + clean.substring(1);
        }

        // Если после всех преобразований номер имеет от 10 до 15 цифр, считаем его валидным международным
        if (clean.length() >= 10 && clean.length() <= 15) {
            return clean;
        }

        throw new InvalidPhoneException(phoneRaw);
    }

    /**
     * Проверяет, является ли строка валидным ПИН/ИНН Кыргызстана (ровно 14 цифр).
     *
     * @param pin строка для проверки
     * @return {@code true} если ПИН валиден
     */
    public boolean isValidPin(String pin) {
        return pin != null && pin.trim().matches("^\\d{14}$");
    }
}
