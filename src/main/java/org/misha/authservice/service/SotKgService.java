package org.misha.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис интеграции с порталом проверки должников sot.kg.
 *
 * <p><b>Текущий режим — Вариант А:</b> сервис формирует URL для открытия
 * пользователем в браузере без фонового запроса к порталу.
 *
 * <p><b>Вариант Б (будущее):</b> для автоматической фоновой проверки
 * добавить зависимость {@code spring-boot-starter-webflux} и реализовать
 * {@code WebClient}-запрос к API/форме portal.sot.kg. Интерфейс метода
 * {@link #buildDebtorUrl(String)} останется прежним — изменится только реализация.
 */
@Service
public class SotKgService {

    /** URL портала реестра должников. Можно вынести в application.yml */
    @Value("${sotkg.debtor-url:https://portal.sot.kg/ru/debtors}")
    private String debtorUrl;

    /**
     * Вариант А: возвращает базовый URL реестра должников для открытия пользователем.
     *
     * <p>Портал не принимает ПИН как query-параметр в URL — поиск выполняется
     * через форму на странице. Поэтому ПИН передаётся отдельно в ответе {@code pin},
     * чтобы пользователь мог вставить его вручную.
     *
     * @param pin валидный ПИН из 14 цифр (зарезервирован для Варианта Б)
     * @return URL страницы реестра должников
     */
    public String buildDebtorUrl(String pin) {
        // Вариант Б: здесь будет вызов WebClient к portal.sot.kg
        // с передачей pin и парсингом результата.
        return debtorUrl;
    }
}
