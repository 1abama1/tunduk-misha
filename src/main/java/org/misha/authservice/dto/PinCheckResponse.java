package org.misha.authservice.dto;

/**
 * Ответ на запрос валидации ПИН / ИНН.
 *
 * @param valid     true — ПИН прошёл валидацию (14 цифр)
 * @param pin       нормализованный ПИН (trim) или null, если невалиден
 * @param targetUrl URL для открытия реестра должников (Вариант А)
 *                  или null при невалидном ПИН
 * @param message   сообщение для пользователя
 */
public record PinCheckResponse(
        boolean valid,
        String pin,
        String targetUrl,
        String message
) {

    /** Фабричный метод — успешный результат */
    public static PinCheckResponse ok(String pin, String targetUrl) {
        return new PinCheckResponse(true, pin.trim(), targetUrl, "ПИН валиден");
    }

    /** Фабричный метод — ошибка валидации */
    public static PinCheckResponse invalid() {
        return new PinCheckResponse(false, null, null,
                "ПИН должен состоять ровно из 14 цифр");
    }
}
