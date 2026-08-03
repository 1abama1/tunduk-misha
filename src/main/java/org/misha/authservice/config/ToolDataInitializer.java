package org.misha.authservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Инициализатор инструментов — данные переведены в Flyway-миграции:
 * <ul>
 *   <li>V2__seed_branches.sql — филиалы</li>
 *   <li>V3__seed_tool_catalog.sql — категории, шаблоны, инструменты</li>
 * </ul>
 * Класс оставлен для обратной совместимости с {@link DataInitializer}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolDataInitializer {

    public void init() {
        log.info("ToolDataInitializer: seed data is managed by Flyway migrations (V2, V3).");
    }
}
