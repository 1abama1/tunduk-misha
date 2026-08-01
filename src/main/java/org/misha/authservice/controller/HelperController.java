package org.misha.authservice.controller;

import org.misha.authservice.dto.PinCheckResponse;
import org.misha.authservice.dto.WhatsAppResponse;
import org.misha.authservice.service.HelperService;
import org.misha.authservice.service.SotKgService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер вспомогательных утилит.
 *
 * <p>Базовый путь: {@code /api/v1/tools}
 *
 * <ul>
 *   <li>{@code GET /whatsapp?phone=} — нормализация номера и генерация ссылки WhatsApp</li>
 *   <li>{@code GET /check-pin?pin=}  — валидация ПИН/ИНН и получение URL реестра должников</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/tools")
public class HelperController {

    private final HelperService helperService;
    private final SotKgService  sotKgService;

    public HelperController(HelperService helperService, SotKgService sotKgService) {
        this.helperService = helperService;
        this.sotKgService  = sotKgService;
    }

    /**
     * Нормализует номер телефона КР и возвращает ссылку для открытия чата WhatsApp.
     *
     * <p>Примеры запросов:
     * <pre>
     *   GET /api/v1/tools/whatsapp?phone=0700123456
     *   GET /api/v1/tools/whatsapp?phone=700123456
     *   GET /api/v1/tools/whatsapp?phone=996700123456
     *   GET /api/v1/tools/whatsapp?phone=0700%20123%20456
     * </pre>
     *
     * <p>Пример успешного ответа {@code 200 OK}:
     * <pre>
     * {
     *   "phone": "996700123456",
     *   "url":   "https://wa.me/996700123456"
     * }
     * </pre>
     *
     * <p>При некорректном номере — {@code 400 Bad Request} через
     * {@link org.misha.authservice.exception.GlobalExceptionHandler}.
     *
     * @param phone сырой номер телефона (пробелы, тире, скобки допустимы)
     * @return {@link WhatsAppResponse} с нормализованным номером и URL
     */
    @GetMapping("/whatsapp")
    public ResponseEntity<WhatsAppResponse> getWhatsAppLink(
            @RequestParam("phone") String phone) {

        String url           = helperService.generateWhatsAppUrl(phone);
        String normalizedNum = helperService.normalizePhone(phone);

        return ResponseEntity.ok(new WhatsAppResponse(normalizedNum, url));
    }

    /**
     * Проверяет валидность ПИН/ИНН (ровно 14 цифр) и возвращает URL
     * реестра должников для открытия пользователем.
     *
     * <p>Примеры запросов:
     * <pre>
     *   GET /api/v1/tools/check-pin?pin=12345678901234
     * </pre>
     *
     * <p>Пример успешного ответа {@code 200 OK}:
     * <pre>
     * {
     *   "valid":     true,
     *   "pin":       "12345678901234",
     *   "targetUrl": "https://portal.sot.kg/ru/debtors",
     *   "message":   "ПИН валиден"
     * }
     * </pre>
     *
     * <p>Пример ответа при невалидном ПИН {@code 400 Bad Request}:
     * <pre>
     * {
     *   "valid":     false,
     *   "pin":       null,
     *   "targetUrl": null,
     *   "message":   "ПИН должен состоять ровно из 14 цифр"
     * }
     * </pre>
     *
     * @param pin строка ПИН/ИНН для проверки
     * @return {@link PinCheckResponse} со статусом валидации и targetUrl
     */
    @GetMapping("/check-pin")
    public ResponseEntity<PinCheckResponse> checkPin(
            @RequestParam("pin") String pin) {

        if (!helperService.isValidPin(pin)) {
            return ResponseEntity
                    .badRequest()
                    .body(PinCheckResponse.invalid());
        }

        String targetUrl = sotKgService.buildDebtorUrl(pin);
        return ResponseEntity.ok(PinCheckResponse.ok(pin, targetUrl));
    }
}
