package org.misha.authservice.dto;

/**
 * Ответ на запрос генерации ссылки WhatsApp.
 *
 * @param phone нормализованный номер в формате 996XXXXXXXXX
 * @param url   готовая ссылка вида https://wa.me/996XXXXXXXXX
 */
public record WhatsAppResponse(
        String phone,
        String url
) {}
